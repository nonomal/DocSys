package com.DocSystem.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 初始化协调器 —— 合并部署后的统一入口。
 *
 * <p>DocSysAgent 原先各组件用 {@code @PostConstruct} 在 Spring 容器启动阶段
 * 各自抢跑初始化（建表、LLM 同步、环境探测、任务恢复）。合并进 DocSys 后，
 * 这与 DocSys 的初始化模型冲突：DocSys 采用延迟 + force 分级的 {@code docSysInit}
 * —— 系统启动时若数据库不存在只写标记、不创建，真正的建库发生在用户经安装页
 * 触发的 {@code docSysInit(true)}。Agent 若在容器 refresh 阶段就抢着建自己的表 /
 * 同步配置，会在数据库尚不存在或类型不匹配（如 SQLite）时报错并阻塞启动。
 *
 * <p>因此所有 Agent 侧的重初始化都改由 DocSys 的 {@code docSysInit} 成功路径
 * 统一、同步地调用本类的 {@link #initAfterDocSysReady()}，与 DocSys 同节奏：
 * 只有在 DocSys 确认数据库就绪（force=true 建库成功，或库已存在正常启动）后才执行。
 *
 * <p>本类只协调两件事，均幂等：
 * <ol>
 *   <li>{@link DatabaseInitializer#init()} —— CREATE TABLE IF NOT EXISTS 建 agent_* 表</li>
 *   <li>{@link LlmConfigSyncService#syncLlmConfigFromDocSys()} —— 从 DocSys 内存配置同步 LLM</li>
 * </ol>
 * 任务恢复（原 MainAgent.resumePendingTasks）已废弃，不在此触发。
 */
@Component
public class AgentInitService {

    private static final Logger log = LoggerFactory.getLogger(AgentInitService.class);

    /** 保证只初始化一次（docSysInit 在某些路径下可能被多次调用）。 */
    private volatile boolean initialized = false;

    @Autowired(required = false)
    private DatabaseInitializer databaseInitializer;

    @Autowired(required = false)
    private LlmConfigSyncService llmConfigSyncService;

    /**
     * 在 DocSys 确认数据库就绪后触发 Agent 初始化。
     * 由 {@code BaseController.docSysInit(...)} 的成功路径调用。
     * 全程 try/catch 兜底：Agent 初始化失败不得影响 DocSys 主流程启动。
     */
    public synchronized void initAfterDocSysReady() {
        if (initialized) {
            log.debug("AgentInitService 已初始化，跳过重复调用");
            return;
        }
        log.info("=== DocSysAgent 初始化开始（由 docSysInit 触发）===");

        // 1) 建表（幂等：CREATE TABLE IF NOT EXISTS）
        if (databaseInitializer != null) {
            try {
                databaseInitializer.init();
            } catch (Exception e) {
                log.error("Agent 数据库表初始化失败（不影响 DocSys 启动）: {}", e.getMessage(), e);
            }
        } else {
            log.warn("DatabaseInitializer 未注入，跳过 Agent 建表");
        }

        // 2) LLM 配置同步（直接读 DocSys 内存配置，无网络阻塞）
        if (llmConfigSyncService != null) {
            try {
                llmConfigSyncService.syncLlmConfigFromDocSys();
            } catch (Exception e) {
                log.error("Agent LLM 配置同步失败（不影响 DocSys 启动）: {}", e.getMessage(), e);
            }
        } else {
            log.warn("LlmConfigSyncService 未注入，跳过 LLM 同步");
        }

        initialized = true;
        log.info("=== DocSysAgent 初始化完成 ===");
    }

    /**
     * 强制重建 Agent 数据库表(CREATE TABLE IF NOT EXISTS，幂等)。
     * 供 DocSys 管理后台 resetDatabase 在删表后调用 —— 重置会 DROP 掉 Agent 表,
     * 需立即按当前 schema 重建,避免用户重置后不重启就使用 Agent 时表不存在报错。
     * 不受 initAfterDocSysReady 的一次性标志约束。
     */
    public void rebuildTables() {
        if (databaseInitializer == null) {
            log.warn("DatabaseInitializer 未注入，跳过 Agent 表重建");
            return;
        }
        try {
            log.info("=== DocSysAgent 重建数据库表（由 resetDatabase 触发）===");
            databaseInitializer.init();
        } catch (Exception e) {
            log.error("Agent 数据库表重建失败（不影响 DocSys）: {}", e.getMessage(), e);
        }
    }
}
