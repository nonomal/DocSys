package com.DocSystem.agent.audit;

import java.util.Date;

/**
 * Step 审计实体 —— 映射 agent_step_audits 表（T8.5）。
 * 记录工具链中每一步工具调用的结构化审计（轮次/工具/参数摘要/结果摘要/耗时）。
 */
public class StepAuditEntity {

    private Long id;
    /** 请求 ID（traceId/MDC requestId），用于关联一次工具链请求 */
    private String requestId;
    /** Agent 会话 ID */
    private String sessionId;
    /** 工具链轮次（从 1 开始） */
    private Integer turn;
    /** 工具名 */
    private String tool;
    /** 参数摘要（JSON，截断防膨胀） */
    private String argsSummary;
    /** 结果摘要（success=成功摘要 / failed=错误信息） */
    private String resultSummary;
    /** 是否成功 */
    private Boolean success;
    /** 工具执行耗时（毫秒） */
    private Long durationMs;
    private Date createdAt;

    public StepAuditEntity() {}

    public StepAuditEntity(String requestId, String sessionId, Integer turn, String tool,
                           String argsSummary, String resultSummary, Boolean success, Long durationMs) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.turn = turn;
        this.tool = tool;
        this.argsSummary = argsSummary;
        this.resultSummary = resultSummary;
        this.success = success;
        this.durationMs = durationMs;
        this.createdAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Integer getTurn() { return turn; }
    public void setTurn(Integer turn) { this.turn = turn; }

    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }

    public String getArgsSummary() { return argsSummary; }
    public void setArgsSummary(String argsSummary) { this.argsSummary = argsSummary; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
