package com.DocSystem.agent.memory;

import java.util.Date;

/**
 * 用户记忆实体 —— 映射 agent_user_memory 表（T8.3）。
 *
 * <p>updatedAt 用 {@link Date} 而非 LocalDateTime：项目 MyBatis 为 3.1.1，
 * 无 JSR-310 TypeHandler（LocalDateTime 读取会抛异常导致 selectByKey 失败）。</p>
 */
public class UserMemoryEntity {

    private Long id;
    /** 用户名（DocSystem 登录名） */
    private String username;
    /** 记忆键（如 user.preferred_language） */
    private String memKey;
    /** 记忆值（偏好/上下文文本） */
    private String memValue;
    private Date updatedAt;

    public UserMemoryEntity() {}

    public UserMemoryEntity(String username, String memKey, String memValue) {
        this.username = username;
        this.memKey = memKey;
        this.memValue = memValue;
        this.updatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMemKey() { return memKey; }
    public void setMemKey(String memKey) { this.memKey = memKey; }

    public String getMemValue() { return memValue; }
    public void setMemValue(String memValue) { this.memValue = memValue; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
