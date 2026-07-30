package com.DocSystem.agent.learning.entity;

import java.time.LocalDateTime;

/**
 * User Custom LLM Model - 用户自定义 LLM 模型
 *
 * 每个用户可添加多个自己的 LLM 模型，仅对本人可见（owner = login user name）。
 * 字段对应 Agent 原「设置」弹窗的 LLM 配置：name / endpoint / modelName / apiKey。
 * apiKey 明文存储（与系统 LLM 配置一致），但绝不回传浏览器。
 */
public class UserCustomLlmModel {

    private Long id;
    private String userId;
    private String tenantId;
    private String name;
    private String modelName;
    private String endpoint;
    private String apiKey;
    private String settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
