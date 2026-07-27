package com.docsys.agent.config;

import com.docsys.agent.client.DocSysClient;
import com.docsys.agent.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM Configuration Synchronization Service
 *
 * 自动从 DocSystem 获取 AI 模型配置并同步到 DocSysAgent
 *
 * 策略：
 * - 启动时同步一次（@PostConstruct）
 * - LLM调用失败时自动尝试刷新配置
 * - 配置刷新有最小间隔限制（避免频繁请求）
 *
 * 配置优先级：
 * 1. DocSystem AI 模型配置（最高优先级）
 * 2. 环境变量（LLM_ENDPOINT, LLM_MODEL, LLM_API_KEY）
 * 3. application.yml 默认值
 */
@Component
public class LlmConfigSyncService {

    private static final Logger log = LoggerFactory.getLogger(LlmConfigSyncService.class);

    /** 配置刷新最小间隔（毫秒）: 5分钟 */
    private static final long MIN_REFRESH_INTERVAL = 5 * 60 * 1000;

    @Value("${docsys.url:http://localhost:8100/DocSystem}")
    private String docsysUrl;

    @Value("${docsys.api-key:test-key}")
    private String docsysApiKey;

    @Value("${docsys.admin.username:admin}")
    private String adminUsername;

    @Value("${docsys.admin.password:123456}")
    private String adminPassword;

    @Value("${llm.endpoint:}")
    private String envEndpoint;

    @Value("${llm.model:}")
    private String envModel;

    @Value("${llm.api-key:}")
    private String envApiKey;

    @Autowired(required = false)
    private LLMService llmService;

    /** 上次刷新时间戳 */
    private final AtomicLong lastRefreshTime = new AtomicLong(0);

    /** 上次刷新是否成功 */
    private volatile boolean lastRefreshSuccessful = false;

    /**
     * 初始化时从 DocSystem 同步 AI 模型配置（仅执行一次）
     */
    @PostConstruct
    public void syncLlmConfigFromDocSys() {
        log.info("=== 启动时 LLM 配置同步 ===");
        doSyncConfig();
    }

    /**
     * LLM 调用失败时尝试刷新配置
     *
     * 调用时机：
     * - LLMService 调用失败时
     * - 网络错误时
     * - API Key 失效时
     *
     * 限制：
     * - 最小间隔 5 分钟
     * - 避免频繁请求 DocSystem
     *
     * @param errorMessage LLM 调用失败的错误信息
     */
    public void tryRefreshOnFailure(String errorMessage) {
        long now = System.currentTimeMillis();
        long lastTime = lastRefreshTime.get();

        // 检查是否在最小间隔内
        if (now - lastTime < MIN_REFRESH_INTERVAL) {
            log.debug("距上次刷新不足 {} ms，跳过自动刷新。错误: {}", MIN_REFRESH_INTERVAL, errorMessage);
            return;
        }

        log.warn("⚠️ LLM 调用失败: {}，尝试从 DocSystem 刷新配置", errorMessage);
        boolean success = doSyncConfig();

        if (success) {
            log.info("✅ 配置刷新成功，LLM 调用可能已修复");
        } else {
            log.warn("⚠️ 配置刷新失败，LLM 调用可能仍有问题");
        }
    }

    /**
     * 执行配置同步（内部方法）
     *
     * @return true if sync successful, false otherwise
     */
    private boolean doSyncConfig() {
        long now = System.currentTimeMillis();

        try {
            log.info("开始从 DocSystem 同步 LLM 配置...");

            // 创建 DocSysClient
            DocSysClient docSysClient = new DocSysClient(docsysUrl);

            // 使用管理员账号登录 DocSystem
            log.debug("使用管理员账号登录 DocSystem: {}", adminUsername);
            Map<String, Object> loginResponse = docSysClient.login(adminUsername, adminPassword);

            if (loginResponse == null || !"ok".equals(loginResponse.get("status"))) {
                String loginError = loginResponse != null ? (String) loginResponse.get("msgInfo") : "登录失败";
                log.warn("DocSystem 登录失败: {}，使用环境变量配置", loginError);
                applyEnvConfig();
                updateRefreshStatus(false);
                return false;
            }

            log.debug("DocSystem 登录成功");

            // 调用 DocSystem 的 getAiModelList 接口
            Map<String, Object> response = docSysClient.getAiModelList();

            if (response != null && "ok".equals(response.get("status"))) {
                List<Map<String, Object>> aiModels = (List<Map<String, Object>>) response.get("data");

                if (aiModels != null && !aiModels.isEmpty()) {
                    // 获取第一个可用的 AI 模型配置
                    Map<String, Object> modelConfig = aiModels.get(0);

                    String endpoint = getStringValue(modelConfig, "endpoint");
                    String model = getStringValue(modelConfig, "modelName");
                    String apiKey = getStringValue(modelConfig, "apiKey");

                    log.info("从 DocSystem 获取到 AI 模型配置:");
                    log.info("  Endpoint: {}", endpoint);
                    log.info("  Model: {}", model);
                    log.info("  API Key: {}", apiKey != null ? maskApiKey(apiKey) : "未设置");

                    // 应用配置到 LLMService
                    if (llmService != null) {
                        applyConfig(endpoint, model, apiKey);
                        log.info("✅ LLM 配置已从 DocSystem 同步成功");
                        updateRefreshStatus(true);
                        return true;
                    } else {
                        log.warn("LLMService 未注入，无法应用配置");
                        updateRefreshStatus(false);
                        return false;
                    }
                } else {
                    log.warn("DocSystem 中没有配置 AI 模型，使用环境变量配置");
                    applyEnvConfig();
                    updateRefreshStatus(false);
                    return false;
                }
            } else {
                String msgInfo = response != null ? (String) response.get("msgInfo") : "未知错误";
                log.warn("从 DocSystem 获取 AI 模型列表失败: {}，使用环境变量配置", msgInfo);
                applyEnvConfig();
                updateRefreshStatus(false);
                return false;
            }

        } catch (Exception e) {
            log.error("同步 DocSystem LLM 配置时发生异常: {}，使用环境变量配置", e.getMessage());
            log.debug("异常详情:", e);
            applyEnvConfig();
            updateRefreshStatus(false);
            return false;
        }
    }

    /**
     * 更新刷新状态和时间戳
     */
    private void updateRefreshStatus(boolean success) {
        lastRefreshTime.set(System.currentTimeMillis());
        lastRefreshSuccessful = success;
    }

    /**
     * 应用 DocSystem 配置到 LLMService
     */
    private void applyConfig(String endpoint, String model, String apiKey) {
        if (llmService == null) {
            log.warn("LLMService 未注入，无法应用配置");
            return;
        }

        // 使用 DocSystem 配置，如果为空则回退到环境变量
        String finalEndpoint = (endpoint != null && !endpoint.isEmpty()) ? endpoint : envEndpoint;
        String finalModel = (model != null && !model.isEmpty()) ? model : envModel;
        String finalApiKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : envApiKey;

        // 检查是否所有必要配置都为空
        if ((finalEndpoint == null || finalEndpoint.isEmpty()) &&
            (envEndpoint == null || envEndpoint.isEmpty())) {
            log.warn("DocSystem 和环境变量都没有配置 LLM endpoint，使用默认配置");
            return; // 使用 application.yml 中的默认值
        }

        llmService.applyConfig(finalEndpoint, finalModel, finalApiKey);
        log.info("应用配置: endpoint={}, model={}, apiKey={}",
                finalEndpoint, finalModel, finalApiKey != null ? maskApiKey(finalApiKey) : "未设置");
    }

    /**
     * 应用环境变量配置
     */
    private void applyEnvConfig() {
        if (llmService == null) {
            return;
        }

        if (envEndpoint != null && !envEndpoint.isEmpty()) {
            log.info("使用环境变量配置: endpoint={}, model={}", envEndpoint, envModel);
            llmService.applyConfig(envEndpoint, envModel, envApiKey);
        } else {
            log.info("环境变量未设置，使用 application.yml 默认配置");
        }
    }

    /**
     * 从 Map 中安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 遮蔽 API Key 用于日志输出
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "未设置";
        }
        if (apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 获取上次刷新状态（用于监控和调试）
     */
    public boolean isLastRefreshSuccessful() {
        return lastRefreshSuccessful;
    }

    /**
     * 获取距上次刷新的时间（毫秒）
     */
    public long getTimeSinceLastRefresh() {
        long lastTime = lastRefreshTime.get();
        if (lastTime == 0) {
            return -1; // 从未刷新
        }
        return System.currentTimeMillis() - lastTime;
    }
}
