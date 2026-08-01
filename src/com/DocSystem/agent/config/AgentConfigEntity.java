package com.DocSystem.agent.config;

import java.util.Date;

/**
 * Agent 配置实体 —— 映射 agent_config 表（T8.6）。
 * 用于管理员配置 Agent 行为（如 system prompt override/suffix）。
 *
 * <p>scope 预留"作用域"扩展：global=全局；将来可扩展为按仓库（scope=仓库ID），
 * 满足 MxsDoc 多仓库不同用途分别配置提示词的需求。</p>
 */
public class AgentConfigEntity {

    private Long id;
    /** 作用域：global（当前）；预留 reposId 扩展 */
    private String scope;
    /** 配置键（如 system_prompt_override / system_prompt_suffix） */
    private String configKey;
    /** 配置值 */
    private String configValue;
    private Date updatedAt;

    public AgentConfigEntity() {}

    public AgentConfigEntity(String scope, String configKey, String configValue) {
        this.scope = scope;
        this.configKey = configKey;
        this.configValue = configValue;
        this.updatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
