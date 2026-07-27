package com.docsys.agent.session;

import java.time.LocalDateTime;

/**
 * Session entity for MyBatis mapping.
 * Maps to the agent_sessions table in MySQL.
 */
public class SessionEntity {

    private Long id;
    private String sessionId;
    private String username;
    private String jsessionid;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    private LocalDateTime expiresAt;
    private String metadata;

    public SessionEntity() {}

    public SessionEntity(String sessionId, String username, String jsessionid) {
        this.sessionId = sessionId;
        this.username = username;
        this.jsessionid = jsessionid;
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getJsessionid() { return jsessionid; }
    public void setJsessionid(String jsessionid) { this.jsessionid = jsessionid; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}