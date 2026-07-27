package com.docsys.agent.controller;

import com.docsys.agent.evolution.ExperienceMemory;
import com.docsys.agent.evolution.SelfDiagnostics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Evolution API - 自主进化系统接口
 *
 * 提供 Soul 达尔文进化系统的监控和管理接口
 */
@RestController
@RequestMapping("/agent/api/evolution")
public class EvolutionController {

    private final ExperienceMemory experienceMemory;
    private final SelfDiagnostics selfDiagnostics;

    @Autowired
    public EvolutionController(ExperienceMemory experienceMemory, SelfDiagnostics selfDiagnostics) {
        this.experienceMemory = experienceMemory;
        this.selfDiagnostics = selfDiagnostics;
    }

    /**
     * 获取完整学习统计（主接口）
     * 返回经验系统和诊断系统的综合数据
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 经验系统统计
        Map<String, Object> expStats = experienceMemory.getStats();
        stats.put("experience", expStats);

        // 诊断系统状态
        Map<String, Object> diagStats = selfDiagnostics.getHealthStatus();
        stats.put("diagnostics", diagStats);

        // 计算进化等级 (基于模式数量)
        int patterns = (int) expStats.getOrDefault("intentPatterns", 0);
        int level = Math.min(10, patterns / 3 + 1);
        stats.put("evolutionLevel", level);

        // 计算进化进度 (0-100%)
        int progress = Math.min(100, patterns * 5);
        stats.put("evolutionProgress", progress);

        return stats;
    }

    /**
     * 获取意图模式列表
     */
    @GetMapping("/patterns")
    public Map<String, Object> getPatterns() {
        Map<String, Object> result = new HashMap<>();
        result.put("patterns", experienceMemory.getAllPatterns());
        return result;
    }

    /**
     * 获取特定意图详情
     */
    @GetMapping("/patterns/{intent}")
    public Map<String, Object> getPattern(@PathVariable String intent) {
        ExperienceMemory.IntentPattern pattern = experienceMemory.getPattern(intent);
        if (pattern == null) {
            Map<String, Object> errData = new HashMap<>();
            errData.put("error", "Pattern not found for intent: " + intent);
            return errData;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("intent", pattern.intent);
        result.put("pattern", pattern.pattern);
        result.put("successCount", pattern.successCount);
        result.put("lastSeen", pattern.lastSeen);
        result.put("sampleQueries", pattern.sampleQueries.size());
        result.put("keywords", pattern.keywords);
        return result;
    }

    /**
     * 获取性能报告
     */
    @GetMapping("/performance")
    public Map<String, Object> getPerformance() {
        Map<String, Object> report = selfDiagnostics.getPerformanceReport();
        report.put("recommendations", selfDiagnostics.getRecommendations());
        return report;
    }

    /**
     * 获取健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> health = selfDiagnostics.getHealthStatus();
        health.put("recommendations", selfDiagnostics.getRecommendations());
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    /**
     * 获取失败分析
     */
    @GetMapping("/failures")
    public Map<String, Object> getFailures() {
        return experienceMemory.analyzeFailures();
    }

    /**
     * 清除告警
     */
    @PostMapping("/alerts/clear")
    public Map<String, Object> clearAlerts() {
        selfDiagnostics.clearAlerts();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Alerts cleared");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
