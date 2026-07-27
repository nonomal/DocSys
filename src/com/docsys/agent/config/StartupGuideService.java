package com.docsys.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 启动引导服务
 *
 * 应用启动时的第一道工序，负责环境探索和初始化引导：
 * 1. 自动探索 MySQL 和 DocSystem 服务
 * 2. 初始化数据库表（仅一次）
 * 3. 同步 LLM 配置（启动时一次，失败时自动重试）
 * 4. 输出友好的配置指南
 */
@Component
@DependsOn({"adaptiveDeploymentService", "databaseInitializer", "llmConfigSyncService"})
public class StartupGuideService {

    private static final Logger log = LoggerFactory.getLogger(StartupGuideService.class);

    @Autowired
    private AdaptiveDeploymentService adaptiveDeploymentService;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Autowired
    private LlmConfigSyncService llmConfigSyncService;

    @Value("${docsys.auto-discovery:true}")
    private boolean autoDiscoveryEnabled;

    @Value("${docsys.show-banner:true}")
    private boolean showBanner;

    /**
     * 应用启动时执行环境探索和初始化
     */
    @PostConstruct
    public void onStartup() {
        if (!autoDiscoveryEnabled) {
            log.info("自动探索已禁用 (docsys.auto-discovery=false)，跳过环境发现");
            return;
        }

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║          DocSysAgent 环境自适应探索                            ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // 步骤1: 环境探索
        AdaptiveDeploymentService.DiscoveryResult result = adaptiveDeploymentService.discoverEnvironment();

        // 步骤2: 显示配置指南
        if (showBanner) {
            String guide = adaptiveDeploymentService.getConfigurationGuide(result);
            log.info("\n{}", guide);
        }

        // 步骤3: 数据库初始化（仅一次，由 DatabaseInitializer 的 @PostConstruct 处理）
        if (result.mysqlFound) {
            log.info("✅ MySQL 已发现，数据库表将自动初始化");
        } else {
            log.warn("⚠️ MySQL 未发现，数据库表将不会初始化");
        }

        // 步骤4: LLM 配置同步（由 LlmConfigSyncService 的 @PostConstruct 处理）
        if (result.docSysFound && result.docSysLoginSuccess) {
            log.info("✅ DocSystem 已发现且登录成功，LLM 配置将自动同步");
        } else if (result.docSysFound) {
            log.warn("⚠️ DocSystem 已发现但登录失败，LLM 配置将使用环境变量");
        } else {
            log.warn("⚠️ DocSystem 未发现，LLM 配置将使用环境变量");
        }

        log.info("═══════════════════════════════════════════════════════════════════");
        log.info("启动引导完成");
        log.info("═══════════════════════════════════════════════════════════════════");
    }
}
