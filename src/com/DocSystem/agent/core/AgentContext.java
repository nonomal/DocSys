package com.DocSystem.agent.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Context - holds session and user information
 * Builder pattern for construction
 */
public class AgentContext {
    private String sessionId;
    private String userId;
    private String username;
    private Integer role;
    private Map<String, Object> attributes;
    private Long createdAt;
    private Long lastActivityAt;

    private AgentContext() {
        this.attributes = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentContext context = new AgentContext();

        public Builder withSessionId(String sessionId) {
            context.sessionId = sessionId;
            return this;
        }

        public Builder withUserId(String userId) {
            context.userId = userId;
            return this;
        }

        public Builder withUsername(String username) {
            context.username = username;
            return this;
        }

        public Builder withRole(Integer role) {
            context.role = role;
            return this;
        }

        public Builder withAttribute(String key, Object value) {
            context.attributes.put(key, value);
            return this;
        }

        public AgentContext build() {
            context.lastActivityAt = System.currentTimeMillis();
            return context;
        }
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public Integer getRole() { return role; }
    public Map<String, Object> getAttributes() { return attributes; }
    public Long getCreatedAt() { return createdAt; }
    public Long getLastActivityAt() { return lastActivityAt; }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
        this.lastActivityAt = System.currentTimeMillis();
    }

    public boolean isAdmin() {
        return role != null && role == 1;
    }

    public boolean isSuperAdmin() {
        return role != null && role == 2;
    }
}
