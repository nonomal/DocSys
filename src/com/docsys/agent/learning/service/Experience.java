package com.docsys.agent.learning.service;

import java.util.List;

/**
 * Experience - 用户经验
 */
public class Experience {
    private String intent;
    private List<String> actions;
    private boolean success;
    private Double successRate;
    private Double recencyScore;
    private String userId;
    private String taskInput;
    private String taskOutput;
    private Long durationMs;
    private Integer rating;
    private String feedback;
    private List<String> tags;

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    public Double getRecencyScore() { return recencyScore; }
    public void setRecencyScore(Double recencyScore) { this.recencyScore = recencyScore; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTaskInput() { return taskInput; }
    public void setTaskInput(String taskInput) { this.taskInput = taskInput; }
    public String getTaskOutput() { return taskOutput; }
    public void setTaskOutput(String taskOutput) { this.taskOutput = taskOutput; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
