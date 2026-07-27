package com.docsys.agent.learning.entity;

import java.time.LocalDateTime;

/**
 * User Behavior Tag Entity - 用户行为标签
 * POJO for MyBatis mapping.
 */
public class UserBehaviorTag {
    private Long id;
    private String userId;
    private String tenantId;
    private String tag;
    private Double weight = 1.0;
    private Integer actionCount = 0;
    private LocalDateTime lastActionAt;
    private LocalDateTime createdAt;

    public UserBehaviorTag() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Integer getActionCount() { return actionCount; }
    public void setActionCount(Integer actionCount) { this.actionCount = actionCount; }
    public LocalDateTime getLastActionAt() { return lastActionAt; }
    public void setLastActionAt(LocalDateTime lastActionAt) { this.lastActionAt = lastActionAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}