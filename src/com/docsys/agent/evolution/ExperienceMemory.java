package com.docsys.agent.evolution;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * ExperienceMemory - 经验记忆系统
 *
 * 核心功能：
 * 1. 记录成功/失败执行经验
 * 2. 从经验中提取模式
 * 3. 提供经验查询接口
 * 4. 持久化学习成果
 *
 * 设计原则：
 * - 异步记录，不阻塞主流程
 * - 自动清理过期经验（TTL滑动窗口）
 * - 模式聚合提升查询效率
 * - O(1) 意图识别索引（keywordIntentIndex）
 *
 * GA-style 4层记忆架构：
 * - L1: Index (关键词索引, O(1) 查找)
 * - L2: Facts (执行记录, 最近1000条)
 * - L3: SOP (标准操作流程, 成功模式)
 * - L4: Archive (归档, 历史经验)
 */
@Component
public class ExperienceMemory {

    /**
     * GA-style 4层记忆枚举
     * 按需加载策略，减少内存占用
     */
    public enum MemoryLayer {
        L1_INDEX,    // keywordIntentIndex - 关键词索引
        L2_FACTS,   // recentExecutions - 执行记录
        L3_SOP,     // successStrategies - 成功模式
        L4_ARCHIVE   // 持久化归档 - 历史经验
    }

    private static final Logger log = LoggerFactory.getLogger(ExperienceMemory.class);

    // 内存存储
    private final ConcurrentLinkedQueue<ExecutionRecord> recentExecutions = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, IntentPattern> intentPatterns = new ConcurrentHashMap<>();
    // 关键词 -> 意图集合索引，O(1) 查找
    private final ConcurrentHashMap<String, Set<String>> keywordIntentIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> successStrategies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> failurePatterns = new ConcurrentHashMap<>();

    // 配置
    private static final int MAX_RECENT_EXECUTIONS = 1000;
    private static final long EXECUTION_TTL_MS = 24 * 60 * 60 * 1000L; // 24小时过期
    private static final int STRATEGY_MAX_SAMPLES = 20;
    private static final int FAILURE_MAX_SAMPLES = 50;
    private static final String STORAGE_PATH = "data/evolution/";

    // Spring-managed executor for async disk I/O
    private ExecutorService diskExecutor;

    // 公开构造函数（Spring 使用）
    public ExperienceMemory() {
        init();
    }

    // 私有构造函数（fallback 模式使用）
    private ExperienceMemory(boolean fallback) {
        diskExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ExperienceMemory-DiskWriter");
            t.setDaemon(true);
            return t;
        });
        loadFromDisk();
    }

    /** 兼容旧调用方；优先使用 Spring 注入 */
    public static ExperienceMemory getInstance() {
        return getInstanceFallback();
    }

    /** Fallback（支持非 Spring 环境，如单元测试） */
    private static volatile ExperienceMemory _fallback;
    public static ExperienceMemory getInstanceFallback() {
        if (_fallback == null) {
            synchronized (ExperienceMemory.class) {
                if (_fallback == null) {
                    _fallback = new ExperienceMemory(true);
                }
            }
        }
        return _fallback;
    }

    @PostConstruct
    public void init() {
        diskExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ExperienceMemory-DiskWriter");
            t.setDaemon(true);
            return t;
        });
        loadFromDisk();
        log.info("ExperienceMemory initialized with {} patterns, {} strategies",
            intentPatterns.size(), successStrategies.size());
    }

    @PreDestroy
    public void shutdown() {
        if (diskExecutor != null) {
            diskExecutor.shutdown();
            try {
                if (!diskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    diskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                diskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 记录执行结果
     */
    public void recordExecution(String userQuery, String intent, boolean success,
                                long duration, String errorMessage) {
        ExecutionRecord record = new ExecutionRecord(
            System.currentTimeMillis(),
            userQuery,
            intent,
            success,
            duration,
            errorMessage
        );

        recentExecutions.add(record);

        // TTL清理：移除超过24小时的旧记录
        long cutoff = System.currentTimeMillis() - EXECUTION_TTL_MS;
        while (!recentExecutions.isEmpty()) {
            ExecutionRecord oldest = recentExecutions.peek();
            if (oldest == null || oldest.timestamp > cutoff) break;
            recentExecutions.poll();
        }

        // 超过上限时移除最旧的
        while (recentExecutions.size() > MAX_RECENT_EXECUTIONS) {
            recentExecutions.poll();
        }

        // 更新模式统计
        if (success) {
            updateSuccessPattern(userQuery, intent);
        } else {
            updateFailurePattern(userQuery, intent, errorMessage);
        }

        // 异步持久化（每10条触发一次）
        if (recentExecutions.size() % 10 == 0) {
            saveToDiskAsync();
        }
    }

    /**
     * 最近 n 条与给定 intent 匹配的执行记录（不区分大小写）。best-effort，失败返回空列表。
     */
    public List<ExecutionRecord> recentForIntent(String intent, int n) {
        if (n <= 0) return new ArrayList<>();
        try {
            synchronized (recentExecutions) {
                // recentExecutions 是 ConcurrentLinkedQueue,先物化为数组保证多步骤一致
                Object[] arr = recentExecutions.toArray();
                List<ExecutionRecord> out = new java.util.ArrayList<>();
                int cap = n;
                // 从尾到头取最近 n 条
                for (int i = arr.length - 1; i >= 0 && out.size() < cap; i--) {
                    ExecutionRecord r = (ExecutionRecord) arr[i];
                    if (r != null && intent != null
                        && intent.equalsIgnoreCase(r.intent)) {
                        out.add(r);
                    }
                }
                Collections.reverse(out);
                return out;
            }
        } catch (Exception e) {
            log.warn("recentForIntent failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 记录任务分解结果
     */
    public void recordDecomposition(String userQuery, String intent,
                                   List<String> subTasks, boolean success) {
        String key = intent + ":" + subTasks.size();
        successStrategies.computeIfAbsent(key, k -> new ArrayList<>());

        if (success) {
            List<String> strategies = successStrategies.get(key);
            synchronized (strategies) {
                strategies.add(userQuery);
                // 保留最近N个样本
                if (strategies.size() > STRATEGY_MAX_SAMPLES) {
                    strategies.remove(0);
                }
            }
        }
    }

    /**
     * 识别用户意图 - O(1) 关键词索引 + O(k) 候选验证
     */
    public String recognizeIntent(String userQuery) {
        String normalized = normalizeQuery(userQuery);

        // 精确匹配
        for (IntentPattern pattern : intentPatterns.values()) {
            if (pattern.sampleQueries.contains(normalized)) {
                log.debug("Recognized intent '{}' via exact match", pattern.intent);
                return pattern.intent;
            }
        }

        // 关键词匹配：O(k) 从索引查找，而非遍历所有模式
        Set<String> queryKeywords = extractKeywords(normalized);
        String bestIntent = null;
        int bestScore = 0;

        for (String kw : queryKeywords) {
            Set<String> candidateIntents = keywordIntentIndex.get(kw);
            if (candidateIntents == null) continue;
            for (String intent : candidateIntents) {
                IntentPattern pattern = intentPatterns.get(intent);
                if (pattern == null) continue;
                int score = Collections.frequency(queryKeywords, kw);
                if (score > bestScore) {
                    bestScore = score;
                    bestIntent = intent;
                }
            }
        }

        if (bestIntent != null) {
            log.debug("Recognized intent '{}' via keyword match (score={})", bestIntent, bestScore);
        }
        return bestIntent;
    }

    /**
     * GA-style 按需加载识别用户意图
     * 只加载到指定层，减少内存占用
     *
     * @param userQuery 用户查询
     * @param maxLayer 最大加载层（包含）
     * @return 识别的意图，null表示未识别
     */
    public String recognizeIntent(String userQuery, MemoryLayer maxLayer) {
        String normalized = normalizeQuery(userQuery);

        // L1: Index layer - 精确匹配 + 关键词索引
        if (maxLayer.ordinal() >= MemoryLayer.L1_INDEX.ordinal()) {
            for (IntentPattern pattern : intentPatterns.values()) {
                if (pattern.sampleQueries.contains(normalized)) {
                    log.debug("Recognized intent '{}' via L1 exact match", pattern.intent);
                    return pattern.intent;
                }
            }
        }

        // L2: Facts layer - 执行记录验证
        if (maxLayer.ordinal() >= MemoryLayer.L2_FACTS.ordinal()) {
            Set<String> queryKeywords = extractKeywords(normalized);
            String bestIntent = null;
            int bestScore = 0;

            for (String kw : queryKeywords) {
                Set<String> candidateIntents = keywordIntentIndex.get(kw);
                if (candidateIntents == null) continue;
                for (String intent : candidateIntents) {
                    IntentPattern pattern = intentPatterns.get(intent);
                    if (pattern == null) continue;
                    int score = Collections.frequency(queryKeywords, kw);
                    if (score > bestScore) {
                        bestScore = score;
                        bestIntent = intent;
                    }
                }
            }

            if (bestIntent != null) {
                log.debug("Recognized intent '{}' via L1+L2 keyword match (score={})", bestIntent, bestScore);
                return bestIntent;
            }
        }

        // L3: SOP layer - 成功策略验证
        if (maxLayer.ordinal() >= MemoryLayer.L3_SOP.ordinal()) {
            // successStrategies already loaded in memory
            // Could add fuzzy match here if needed
        }

        // L4: Archive layer - 从磁盘归档加载
        if (maxLayer.ordinal() >= MemoryLayer.L4_ARCHIVE.ordinal()) {
            // Load from disk if needed - lazy load archive
            log.trace("L4 archive layer not yet implemented - using L1-L3 only");
        }

        return null;
    }

    /**
     * 获取当前加载层级
     */
    public MemoryLayer getCurrentLayer() {
        if (!intentPatterns.isEmpty() && !keywordIntentIndex.isEmpty()) {
            return MemoryLayer.L1_INDEX;
        }
        if (!recentExecutions.isEmpty()) {
            return MemoryLayer.L2_FACTS;
        }
        if (!successStrategies.isEmpty()) {
            return MemoryLayer.L3_SOP;
        }
        return MemoryLayer.L4_ARCHIVE;
    }

    /**
     * 获取成功的任务分解策略
     */
    public List<String> getSuccessStrategy(String intent, int subTaskCount) {
        String key = intent + ":" + subTaskCount;
        return successStrategies.getOrDefault(key, Collections.emptyList());
    }

    /**
     * 获取失败模式分析
     */
    public Map<String, Object> analyzeFailures() {
        Map<String, Object> analysis = new HashMap<>();

        // 统计失败率（最近100条）
        List<ExecutionRecord> recent = recentExecutions.stream()
            .skip(Math.max(0, recentExecutions.size() - 100))
            .collect(Collectors.toList());
        long failures = recent.stream().filter(r -> !r.success).count();
        analysis.put("failureRate", recent.isEmpty() ? 0.0 : (double) failures / recent.size());
        analysis.put("totalExecutions", recentExecutions.size());

        // 常见失败模式
        Map<String, Long> failureCounts = new HashMap<>();
        for (ExecutionRecord rec : recent) {
            if (!rec.success && rec.errorMessage != null) {
                String category = categorizeError(rec.errorMessage);
                failureCounts.merge(category, 1L, Long::sum);
            }
        }
        analysis.put("failureCategories", failureCounts);

        return analysis;
    }

    /**
     * 获取学习统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", recentExecutions.size());
        stats.put("intentPatterns", intentPatterns.size());
        stats.put("successStrategies", successStrategies.size());

        // 最近50条成功率
        List<ExecutionRecord> recent = recentExecutions.stream()
            .skip(Math.max(0, recentExecutions.size() - 50))
            .collect(Collectors.toList());
        if (!recent.isEmpty()) {
            long success = recent.stream().filter(r -> r.success).count();
            stats.put("recentSuccessRate", (double) success / recent.size());
        }

        return stats;
    }

    /**
     * 获取所有意图模式（供API展示）
     */
    public Collection<IntentPattern> getAllPatterns() {
        return intentPatterns.values();
    }

    /**
     * 获取特定意图的详细信息
     */
    public IntentPattern getPattern(String intent) {
        return intentPatterns.get(intent);
    }

    // ==================== 私有方法 ====================

    private void updateSuccessPattern(String query, String intent) {
        String normalized = normalizeQuery(query);

        IntentPattern pattern = intentPatterns.computeIfAbsent(intent, k -> new IntentPattern(intent));
        pattern.sampleQueries.add(normalized);
        pattern.successCount++;
        pattern.lastSeen = System.currentTimeMillis();

        // 更新关键词索引
        for (String kw : extractKeywords(normalized)) {
            keywordIntentIndex.computeIfAbsent(kw, k -> ConcurrentHashMap.newKeySet()).add(intent);
        }

        // 提取关键词作为模式
        extractKeywords(normalized).forEach(pattern.keywords::add);
    }

    private void updateFailurePattern(String query, String intent, String error) {
        String key = intent + "_failure";
        failurePatterns.computeIfAbsent(key, k -> new ArrayList<>());
        List<String> failures = failurePatterns.get(key);

        synchronized (failures) {
            failures.add(query + "|" + (error != null ? error : "unknown"));
            if (failures.size() > FAILURE_MAX_SAMPLES) {
                failures.remove(0);
            }
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.toLowerCase().trim()
            // 统一中文标点
            .replaceAll("[，。！？、；：]", " ")
            .replaceAll("[.,!?;:'\"()\\[\\]{}]", " ")
            .replaceAll("\\s+", " ");
    }

    private Set<String> extractKeywords(String query) {
        Set<String> keywords = new HashSet<>();
        if (query == null || query.isEmpty()) return keywords;

        // 提取2-6个字符的中文词
        for (int i = 0; i < query.length(); i++) {
            for (int len = 2; len <= 6 && i + len <= query.length(); len++) {
                String sub = query.substring(i, i + len);
                if (!sub.matches(".*\\s.*")) { // 不含空格
                    keywords.add(sub);
                }
            }
        }

        return keywords;
    }

    private String categorizeError(String error) {
        if (error == null) return "unknown";
        error = error.toLowerCase();

        if (error.contains("session") || error.contains("cookie") || error.contains("401")) return "auth";
        if (error.contains("timeout") || error.contains("connection") || error.contains("503")) return "network";
        if (error.contains("null") || error.contains("空") || error.contains("npe")) return "null_pointer";
        if (error.contains("parse") || error.contains("解析") || error.contains("json")) return "parsing";
        if (error.contains("permission") || error.contains("权限") || error.contains("403")) return "permission";
        if (error.contains("not found") || error.contains("404") || error.contains("不存在")) return "not_found";

        return "other";
    }

    private void saveToDiskAsync() {
        if (diskExecutor == null || diskExecutor.isShutdown()) return;
        diskExecutor.submit(() -> {
            try {
                saveToDisk();
            } catch (Exception e) {
                log.warn("Failed to save experience to disk: {}", e.getMessage());
            }
        });
    }

    private void saveToDisk() throws IOException {
        Path dir = Paths.get(STORAGE_PATH);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // 保存模式
        String patternsJson = JSON.toJSONString(intentPatterns.values());
        Files.write(dir.resolve("patterns.json"), patternsJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 保存策略
        String strategiesJson = JSON.toJSONString(successStrategies);
        Files.write(dir.resolve("strategies.json"), strategiesJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 保存失败模式（之前缺失）
        String failuresJson = JSON.toJSONString(failurePatterns);
        Files.write(dir.resolve("failures.json"), failuresJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.debug("Saved {} patterns, {} strategies, {} failure patterns to disk",
            intentPatterns.size(), successStrategies.size(), failurePatterns.size());
    }

    private void loadFromDisk() {
        try {
            Path dir = Paths.get(STORAGE_PATH);
            if (!Files.exists(dir)) return;

            // 加载模式
            Path patternsFile = dir.resolve("patterns.json");
            if (Files.exists(patternsFile)) {
                String json = new String(Files.readAllBytes(patternsFile), StandardCharsets.UTF_8);
                List<IntentPattern> patterns = JSON.parseArray(json, IntentPattern.class);
                for (IntentPattern p : patterns) {
                    intentPatterns.put(p.intent, p);
                    // 重建关键词索引
                    for (String kw : p.keywords) {
                        keywordIntentIndex.computeIfAbsent(kw, k -> ConcurrentHashMap.newKeySet()).add(p.intent);
                    }
                }
            }

            // 加载策略
            Path strategiesFile = dir.resolve("strategies.json");
            if (Files.exists(strategiesFile)) {
                String json = new String(Files.readAllBytes(strategiesFile), StandardCharsets.UTF_8);
                Map<String, List<String>> strategies = JSON.parseObject(json,
                    new TypeReference<Map<String, List<String>>>() {});
                successStrategies.putAll(strategies);
            }

            // 加载失败模式（之前缺失）
            Path failuresFile = dir.resolve("failures.json");
            if (Files.exists(failuresFile)) {
                String json = new String(Files.readAllBytes(failuresFile), StandardCharsets.UTF_8);
                Map<String, List<String>> failures = JSON.parseObject(json,
                    new TypeReference<Map<String, List<String>>>() {});
                failurePatterns.putAll(failures);
            }

        } catch (Exception e) {
            log.warn("Failed to load experience from disk: {}", e.getMessage());
        }
    }

    // ==================== 内部类 ====================

    public static class ExecutionRecord {
        public long timestamp;
        public String userQuery;
        public String intent;
        public boolean success;
        public long duration;
        public String errorMessage;

        public ExecutionRecord(long timestamp, String userQuery, String intent,
                               boolean success, long duration, String errorMessage) {
            this.timestamp = timestamp;
            this.userQuery = userQuery;
            this.intent = intent;
            this.success = success;
            this.duration = duration;
            this.errorMessage = errorMessage;
        }
    }

    public static class IntentPattern implements Serializable {
        public String intent;
        public String pattern;
        public Set<String> sampleQueries = new HashSet<>();
        public Set<String> keywords = new HashSet<>();
        public int successCount;
        public long lastSeen;

        public IntentPattern(String intent) {
            this.intent = intent;
            this.pattern = intent;
        }
    }
}
