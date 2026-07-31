package com.DocSystem.agent.memory;

import java.time.LocalDateTime;

/**
 * 用户记忆实体 —— 映射 agent_user_memory 表（T8.3）。
 */
public class UserMemoryEntity {

    private Long id;
    /** 用户名（DocSystem 登录名） */
    private String username;
    /** 记忆键（如 user.preferred_language） */
    private String memKey;
    /** 记忆值（偏好/上下文文本） */
    private String memValue;
    private LocalDateTime updatedAt;

    public UserMemoryEntity() {}

    public UserMemoryEntity(String username, String memKey, String memValue) {
        this.username = username;
        this.memKey = memKey;
        this.memValue = memValue;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMemKey() { return memKey; }
    public void setMemKey(String memKey) { this.memKey = memKey; }

    public String getMemValue() { return memValue; }
    public void setMemValue(String memValue) { this.memValue = memValue; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
