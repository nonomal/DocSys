package com.docsys.agent.evolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SelfDiagnostics - 自我诊断系统
 *
 * 核心功能：
 * 1. 监控执行指标（滑动窗口）
 * 2. 检测异常模式
 * 3. 自动调整策略
 * 4. 提供健康报告
 *
 * 改进：
 * - 滑动窗口统计（最近N条）替代全局平均
 * - P50/P95/P99 延迟百分位数
 * - 更细粒度的错误分类
 * - 持续性告警去重
 */
@Component
public class SelfDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(SelfDiagnostics.class);

    // 指标存储（每个操作独立的滑动窗口）
    private final ConcurrentHashMap<String, OperationMetrics> metrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private final List<HealthAlert> alerts = new ArrayList<>();
    // 最近 N 条 operation 记录（按时间顺序），用于 reflection 引擎读取证据。
    // 保持独立于 OperationMetrics 滑动窗口以避免影响现有指标统计语义。
    private final List<OperationRecord> operationHistory =
        Collections.synchronizedList(new ArrayList<>());

    // 滑动窗口大小（每个操作最近N条）
    private static final int WINDOW_SIZE = 200;
    // 告警阈值
    private static final double ERROR_RATE_THRESHOLD = 0.3; // 30% 错误率告警
    private static final long RESPONSE_TIME_P95_THRESHOLD = 5000; // 5秒 P95 延迟告警
    private static final int ALERT_QUEUE_SIZE = 100;
    // 去重：同类告警30分钟内不重复触发
    private static final long ALERT_DEDUP_WINDOW_MS = 30 * 60 * 1000L;

    // 上次告警时间（去重）
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    // 公开构造函数（Spring 使用）
    public SelfDiagnostics() {
        init();
    }

    // 私有构造函数（fallback 模式使用）
    private SelfDiagnostics(boolean fallback) {
        initializeMetrics();
        log.info("SelfDiagnostics initialized (fallback mode)");
    }

    /** 兼容旧调用方；优先使用 Spring 注入 */
    public static SelfDiagnostics getInstance() {
        return getInstanceFallback();
    }

    /** Fallback（支持非 Spring 环境） */
    private static volatile SelfDiagnostics _fallback;
    public static SelfDiagnostics getInstanceFallback() {
        if (_fallback == null) {
            synchronized (SelfDiagnostics.class) {
                if (_fallback == null) {
                    _fallback = new SelfDiagnostics(true);
                }
            }
        }
        return _fallback;
    }

    @PostConstruct
    public void init() {
        initializeMetrics();
        log.info("SelfDiagnostics initialized");
    }

    /**
     * 记录执行指标（使用滑动窗口）
     */
    public void recordExecution(String operation, boolean success, long durationMs) {
        OperationMetrics m = metrics.computeIfAbsent(operation, k -> new OperationMetrics(operation));

        m.record(success, durationMs);

        if (!success) {
            incrementErrorCount(operation);
        }

        // 记录到 operationHistory（reflection 引擎读取证据用）；上限 200 条与 WINDOW_SIZE 对齐
        synchronized (operationHistory) {
            operationHistory.add(new OperationRecord(
                System.currentTimeMillis(), operation, success, durationMs));
            while (operationHistory.size() > WINDOW_SIZE) {
                operationHistory.remove(0);
            }
        }

        // 检查是否需要告警（带去重）
        checkThresholdsWithDedup(m);
    }

    /**
     * 最近 n 条与给定 operation 匹配的记录（按时间顺序）。best-effort，失败返回空列表。
     */
    public List<OperationRecord> recentByOperation(String operation, int n) {
        if (n <= 0) return new ArrayList<>();
        try {
            synchronized (operationHistory) {
                List<OperationRecord> out = new ArrayList<>();
                for (int i = operationHistory.size() - 1; i >= 0 && out.size() < n; i--) {
                    OperationRecord r = operationHistory.get(i);
                    if (r != null && operation != null && operation.equals(r.operation)) {
                        out.add(r);
                    }
                }
                Collections.reverse(out);
                return out;
            }
        } catch (Exception e) {
            log.warn("recentByOperation failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();

        // 整体健康度（基于滑动窗口）
        double overallHealth = calculateOverallHealth();
        status.put("health", overallHealth > 0.8 ? "healthy" : (overallHealth > 0.5 ? "degraded" : "unhealthy"));
        status.put("healthScore", String.format("%.2f", overallHealth));

        // 各操作指标
        Map<String, Object> operationMetrics = new HashMap<>();
        for (Map.Entry<String, OperationMetrics> entry : metrics.entrySet()) {
            OperationMetrics m = entry.getValue();
            Map<String, Object> om = new HashMap<>();
            om.put("successRate", String.format("%.2f%%", m.getSuccessRate() * 100));
            om.put("avgDuration", m.getAvgDuration() + "ms");
            om.put("p95Duration", m.getP95Duration() + "ms");
            om.put("totalCount", m.totalCount());
            om.put("errorCount", m.errorCount());
            om.put("windowSize", m.windowSize());
            operationMetrics.put(entry.getKey(), om);
        }
        status.put("operations", operationMetrics);

        // 最近告警
        status.put("recentAlerts", getRecentAlerts(10));

        return status;
    }

    /**
     * 获取性能报告
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();

        List<OperationMetrics> sorted = new ArrayList<>(metrics.values());
        sorted.sort((a, b) -> Long.compare(b.totalCount(), a.totalCount()));

        // Top 5 操作
        List<Map<String, Object>> topOperations = new ArrayList<>();
        for (OperationMetrics m : sorted.subList(0, Math.min(5, sorted.size()))) {
            Map<String, Object> opData = new HashMap<>();
            opData.put("operation", m.operation);
            opData.put("count", m.totalCount());
            opData.put("successRate", String.format("%.2f%%", m.getSuccessRate() * 100));
            opData.put("avgDuration", m.getAvgDuration() + "ms");
            opData.put("p95Duration", m.getP95Duration() + "ms");
            topOperations.add(opData);
        }
        report.put("topOperations", topOperations);

        // 慢操作（P95 > 阈值）
        List<Map<String, Object>> slowOperations = new ArrayList<>();
        for (OperationMetrics m : sorted) {
            if (m.getP95Duration() > RESPONSE_TIME_P95_THRESHOLD) {
                Map<String, Object> opData = new HashMap<>();
                opData.put("operation", m.operation);
                opData.put("p95Duration", m.getP95Duration() + "ms");
                opData.put("avgDuration", m.getAvgDuration() + "ms");
                opData.put("count", m.totalCount());
                slowOperations.add(opData);
            }
        }
        report.put("slowOperations", slowOperations);

        // 错误分析
        Map<String, Integer> errorAnalysis = new HashMap<>();
        errorCounts.forEach((k, v) -> errorAnalysis.put(k, v.get()));
        report.put("errorAnalysis", errorAnalysis);

        return report;
    }

    /**
     * 获取建议
     */
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();

        for (OperationMetrics metric : metrics.values()) {
            // 错误率过高
            if (metric.getSuccessRate() < (1 - ERROR_RATE_THRESHOLD)) {
                recommendations.add(String.format(
                    "[WARN] Operation '%s' error rate %.1f%% (threshold: %.0f%%)",
                    metric.operation, (1 - metric.getSuccessRate()) * 100, ERROR_RATE_THRESHOLD * 100
                ));
            }

            // P95 延迟过长
            if (metric.getP95Duration() > RESPONSE_TIME_P95_THRESHOLD) {
                recommendations.add(String.format(
                    "[SLOW] Operation '%s' P95 latency %dms (threshold: %dms)",
                    metric.operation, metric.getP95Duration(), RESPONSE_TIME_P95_THRESHOLD
                ));
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("[OK] All operations within normal thresholds");
        }

        return recommendations;
    }

    /**
     * 获取最近告警
     */
    public List<HealthAlert> getRecentAlerts(int count) {
        synchronized (alerts) {
            int start = Math.max(0, alerts.size() - count);
            return new ArrayList<>(alerts.subList(start, alerts.size()));
        }
    }

    /**
     * 清除告警
     */
    public void clearAlerts() {
        synchronized (alerts) {
            alerts.clear();
        }
        lastAlertTime.clear();
    }

    // ==================== 私有方法 ====================

    private void initializeMetrics() {
        String[] coreOperations = {
            "login", "list_repos", "search", "get_doc",
            "list_docs", "chat", "task_decomposition"
        };

        for (String op : coreOperations) {
            metrics.put(op, new OperationMetrics(op));
        }
    }

    private void incrementErrorCount(String operation) {
        errorCounts.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 带去重的告警检查
     */
    private void checkThresholdsWithDedup(OperationMetrics metric) {
        long now = System.currentTimeMillis();
        String dedupKey = metric.operation;

        // 错误率检查
        if (metric.getSuccessRate() < (1 - ERROR_RATE_THRESHOLD)) {
            dedupKey = "err_rate:" + metric.operation;
            if (shouldAlert(dedupKey, now)) {
                addAlert("ERROR_RATE", metric.operation,
                    String.format("Error rate %.1f%% exceeds threshold %.0f%%",
                        (1 - metric.getSuccessRate()) * 100, ERROR_RATE_THRESHOLD * 100));
            }
        }

        // P95 延迟检查
        if (metric.getP95Duration() > RESPONSE_TIME_P95_THRESHOLD) {
            dedupKey = "slow:" + metric.operation;
            if (shouldAlert(dedupKey, now)) {
                addAlert("SLOW_RESPONSE", metric.operation,
                    String.format("P95 latency %dms exceeds threshold %dms",
                        metric.getP95Duration(), RESPONSE_TIME_P95_THRESHOLD));
            }
        }
    }

    private boolean shouldAlert(String dedupKey, long now) {
        Long last = lastAlertTime.get(dedupKey);
        if (last == null || (now - last) > ALERT_DEDUP_WINDOW_MS) {
            lastAlertTime.put(dedupKey, now);
            return true;
        }
        return false;
    }

    private void addAlert(String type, String source, String message) {
        synchronized (alerts) {
            alerts.add(new HealthAlert(System.currentTimeMillis(), type, source, message));

            while (alerts.size() > ALERT_QUEUE_SIZE) {
                alerts.remove(0);
            }
        }

        log.warn("Health Alert: {} - {}: {}", type, source, message);
    }

    private double calculateOverallHealth() {
        if (metrics.isEmpty()) return 1.0;

        double totalWeight = 0;
        double healthSum = 0;

        for (OperationMetrics m : metrics.values()) {
            double weight = m.totalCount();
            // 滑动窗口成功率 * 0.7 + 延迟健康度 * 0.3
            double latencyHealth = 1 - Math.min(1.0, m.getP95Duration() / (double) RESPONSE_TIME_P95_THRESHOLD);
            double health = m.getSuccessRate() * 0.7 + latencyHealth * 0.3;

            totalWeight += weight;
            healthSum += health * weight;
        }

        return totalWeight > 0 ? healthSum / totalWeight : 1.0;
    }

    // ==================== 内部类 ====================

    /**
     * 操作指标：使用固定大小滑动窗口记录最近N条执行
     */
    public static class OperationMetrics {
        public final String operation;
        // 滑动窗口（固定大小环形缓冲区）
        private final long[] latencyWindow = new long[WINDOW_SIZE];
        private final boolean[] successWindow = new boolean[WINDOW_SIZE];
        private int windowHead = 0; // 下一条写入位置
        private int windowCount = 0; // 当前窗口内有效条目数

        // 全局计数器（不重置）
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final AtomicLong totalDuration = new AtomicLong(0);

        // 有序延迟列表（用于百分位数计算）
        private final List<Long> sortedLatencies = Collections.synchronizedList(new ArrayList<>(WINDOW_SIZE));

        public OperationMetrics(String operation) {
            this.operation = operation;
        }

        public void record(boolean success, long durationMs) {
            int idx = windowHead % WINDOW_SIZE;
            latencyWindow[idx] = durationMs;
            successWindow[idx] = success;
            windowHead++;
            if (windowCount < WINDOW_SIZE) windowCount++;

            totalCount.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            if (!success) errorCount.incrementAndGet();

            // 维护有序延迟列表（用于百分位数）
            synchronized (sortedLatencies) {
                sortedLatencies.add(durationMs);
                if (sortedLatencies.size() > WINDOW_SIZE) {
                    // 移除最早的值（对应 windowHead - WINDOW_SIZE 位置）
                    long oldest = latencyWindow[(windowHead - WINDOW_SIZE) % WINDOW_SIZE];
                    sortedLatencies.remove(oldest);
                }
            }
        }

        public int totalCount() { return totalCount.get(); }
        public int errorCount() { return errorCount.get(); }
        public int windowSize() { return windowCount; }

        public double getSuccessRate() {
            return windowCount > 0
                ? (double) countWindowSuccess() / windowCount
                : 1.0;
        }

        public long getAvgDuration() {
            return windowCount > 0
                ? countWindowAvgDuration()
                : 0;
        }

        public long getP95Duration() {
            if (sortedLatencies.isEmpty()) return 0;
            synchronized (sortedLatencies) {
                if (sortedLatencies.isEmpty()) return 0;
                List<Long> sorted = new ArrayList<>(sortedLatencies);
                Collections.sort(sorted);
                int p95Idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
                p95Idx = Math.max(0, Math.min(p95Idx, sorted.size() - 1));
                return sorted.get(p95Idx);
            }
        }

        private int countWindowSuccess() {
            int count = 0;
            int start = windowHead - windowCount;
            for (int i = 0; i < windowCount; i++) {
                if (successWindow[(start + i) % WINDOW_SIZE]) count++;
            }
            return count;
        }

        private long countWindowAvgDuration() {
            long sum = 0;
            int start = windowHead - windowCount;
            for (int i = 0; i < windowCount; i++) {
                sum += latencyWindow[(start + i) % WINDOW_SIZE];
            }
            return sum / windowCount;
        }
    }

    /**
     * 单条 operation 执行记录（reflection 引擎读取证据用）。
     */
    public static class OperationRecord {
        public final long timestamp;
        public final String operation;
        public final boolean success;
        public final long durationMs;

        public OperationRecord(long timestamp, String operation, boolean success, long durationMs) {
            this.timestamp = timestamp;
            this.operation = operation;
            this.success = success;
            this.durationMs = durationMs;
        }
    }

    public static class HealthAlert {
        public long timestamp;
        public String type;
        public String source;
        public String message;

        public HealthAlert(long timestamp, String type, String source, String message) {
            this.timestamp = timestamp;
            this.type = type;
            this.source = source;
            this.message = message;
        }
    }
}
