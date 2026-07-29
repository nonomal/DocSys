package com.DocSystem.agent.entity;

import java.time.LocalDateTime;

/**
 * Task entity for retry queue persistence.
 * REL-03: Maps to agent_tasks MySQL table.
 * D-09: State machine: pending -> running -> completed/failed
 * POJO for MyBatis mapping.
 */
public class TaskEntity {

    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        RETRY_PENDING
    }

    private Long id;
    private String taskId;
    private String sessionId;
    private String taskType;
    private String taskParams;
    private TaskStatus status = TaskStatus.PENDING;
    private int retryCount = 0;
    private int maxRetries = 3;
    private String result;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime timeoutAt;
    private String metadata;

    // With-methods (immutable pattern)
    public TaskEntity withTaskId(String taskId) { this.taskId = taskId; return this; }
    public TaskEntity withSessionId(String sessionId) { this.sessionId = sessionId; return this; }
    public TaskEntity withTaskType(String taskType) { this.taskType = taskType; return this; }
    public TaskEntity withTaskParams(String params) { this.taskParams = params; return this; }
    public TaskEntity withStatus(TaskStatus status) { this.status = status; return this; }
    public TaskEntity withRetryCount(int count) { this.retryCount = count; return this; }
    public TaskEntity withMaxRetries(int max) { this.maxRetries = max; return this; }
    public TaskEntity withResult(String result) { this.result = result; return this; }
    public TaskEntity withErrorMessage(String msg) { this.errorMessage = msg; return this; }
    public TaskEntity withMetadata(String metadata) { this.metadata = metadata; return this; }
    public TaskEntity withStartedAt(LocalDateTime t) { this.startedAt = t; return this; }
    public TaskEntity withCompletedAt(LocalDateTime t) { this.completedAt = t; return this; }
    public TaskEntity withTimeoutAt(LocalDateTime t) { this.timeoutAt = t; return this; }

    // Standard getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskParams() { return taskParams; }
    public void setTaskParams(String taskParams) { this.taskParams = taskParams; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getTimeoutAt() { return timeoutAt; }
    public void setTimeoutAt(LocalDateTime timeoutAt) { this.timeoutAt = timeoutAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}