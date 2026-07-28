package com.docsys.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 启动引导服务（合并部署后已停用）
 *
 * 原用于独立部署时的环境探索：探测 MySQL / DocSystem、打印配置指南 banner。
 * 合并进 DocSys 后 Agent 与 DocSys 同进程，这些探测无意义（DocSys 就在本地），
 * 故移除 @PostConstruct 与 @DependsOn，不再于容器启动时执行。
 * 数据库表初始化与 LLM 同步改由 DocSys 的 docSysInit 统一触发（见 AgentInitService）。
 */
@Component
public class StartupGuideService {

    private static final Logger log = LoggerFactory.getLogger(StartupGuideService.class);
}
