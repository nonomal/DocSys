package com.DocSystem.agent.session;

import java.time.LocalDateTime;

/**
 * 会话消息实体 —— 映射 agent_session_messages 表。
 * 用于会话历史持久化（关页面后可续接）。
 */
public class SessionMessageEntity {

    private Long id;
    private String sessionId;
    /** user / assistant / system / tool */
    private String role;
    private String content;
    /** 会话内消息序号（保证顺序） */
    private Integer seq;
    private LocalDateTime createdAt;

    public SessionMessageEntity() {}

    public SessionMessageEntity(String sessionId, String role, String content, Integer seq) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.seq = seq;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
