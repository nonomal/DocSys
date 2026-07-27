package com.docsys.agent.learning.entity;

import java.time.LocalDateTime;

/**
 * User Experience Entity - 用户经验
 * 记录用户成功完成任务的经验
 * POJO for MyBatis mapping.
 */
public class UserExperience {
    private Long id;
    private String userId;
    private String tenantId;
    private String taskType;
    private String taskInput;
    private String taskOutput;
    private Boolean success;
    private Long durationMs;
    private Integer rating;
    private String feedback;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserExperience() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskInput() { return taskInput; }
    public void setTaskInput(String taskInput) { this.taskInput = taskInput; }
    public String getTaskOutput() { return taskOutput; }
    public void setTaskOutput(String taskOutput) { this.taskOutput = taskOutput; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
