package com.DocSystem.agent.config;

import com.DocSystem.agent.client.DocSysClient;
import com.DocSystem.agent.llm.LLMService;
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
     * 从 DocSystem 同步 AI 模型配置。
     * 合并部署后由 DocSys 的 docSysInit 成功路径统一触发（见 AgentInitService），
     * 不再使用 @PostConstruct 在容器启动时抢跑。
     */
    public void syncLlmConfigFromDocSys() {
        log.info("=== LLM 配置同步（由 docSysInit 触发）===");
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
     * 执行配置同步（内部方法）。
     *
     * 合并部署后 Agent 与 DocSys 同进程，直接读取 DocSys 已加载到内存的
     * {@code BaseFunction.systemLLMConfig}（由 DocSys 在类加载静态块 / docSysInit
     * 中从 docSysConfig.properties 解析），不再通过 HTTP 登录 DocSystem +
     * getAiModelList，从根本上消除启动时的网络阻塞。
     *
     * @return true if sync successful, false otherwise
     */
    private boolean doSyncConfig() {
        try {
            log.info("从 DocSys 内存配置同步 LLM（BaseFunction.systemLLMConfig）...");

            com.DocSystem.common.entity.SystemLLMConfig cfg =
                com.DocSystem.common.BaseFunction.systemLLMConfig;

            if (cfg == null || !cfg.enabled
                    || cfg.llmConfigList == null || cfg.llmConfigList.isEmpty()) {
                log.warn("DocSys 未配置可用 LLM（systemLLMConfig 为空/未启用），使用环境变量配置");
                applyEnvConfig();
                updateRefreshStatus(false);
                return false;
            }

            // 取第一个可用模型（与原 getAiModelList 取 index 0 语义一致）
            com.DocSystem.common.entity.LLMConfig model = cfg.llmConfigList.get(0);
            String endpoint = model.url;
            String modelName = model.modelName;
            String apiKey = model.apikey;

            log.info("从 DocSys 获取到 LLM 配置: endpoint={}, model={}, apiKey={}",
                    endpoint, modelName, apiKey != null ? maskApiKey(apiKey) : "未设置");

            if (llmService != null) {
                applyConfig(endpoint, modelName, apiKey);
                log.info("✅ LLM 配置已从 DocSys 内存同步成功");
                updateRefreshStatus(true);
                return true;
            } else {
                log.warn("LLMService 未注入，无法应用配置");
                updateRefreshStatus(false);
                return false;
            }

        } catch (Exception e) {
            log.error("同步 DocSys LLM 配置时发生异常: {}，使用环境变量配置", e.getMessage());
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
