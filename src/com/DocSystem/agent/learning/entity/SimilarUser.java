package com.DocSystem.agent.learning.entity;

/**
 * SimilarUser - 相似用户
 */
public class SimilarUser {
    private String userId;
    private Double similarityScore;
    private Double permissionSimilarity;
    private Double behaviorSimilarity;

    public SimilarUser() {}

    public SimilarUser(String userId, Double similarityScore) {
        this.userId = userId;
        this.similarityScore = similarityScore;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
    public Double getPermissionSimilarity() { return permissionSimilarity; }
    public void setPermissionSimilarity(Double permissionSimilarity) { this.permissionSimilarity = permissionSimilarity; }
    public Double getBehaviorSimilarity() { return behaviorSimilarity; }
    public void setBehaviorSimilarity(Double behaviorSimilarity) { this.behaviorSimilarity = behaviorSimilarity; }
}
