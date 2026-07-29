package com.DocSystem.agent.learning.entity;

import java.util.List;
import java.util.ArrayList;

/**
 * Recommendation - 推荐结果
 */
public class Recommendation {
    private String intent;
    private List<String> actions;
    private Double confidenceScore;
    private String reason;
    private List<String> sourceUserIds;
    private Integer sourceCount;

    public Recommendation() {
        this.sourceUserIds = new ArrayList<>();
    }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getSourceUserIds() { return sourceUserIds; }
    public void setSourceUserIds(List<String> sourceUserIds) { this.sourceUserIds = sourceUserIds; }
    public Integer getSourceCount() { return sourceCount; }
    public void setSourceCount(Integer sourceCount) { this.sourceCount = sourceCount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Recommendation rec = new Recommendation();

        public Builder intent(String intent) {
            rec.setIntent(intent);
            return this;
        }

        public Builder actions(List<String> actions) {
            rec.setActions(actions);
            return this;
        }

        public Builder confidenceScore(Double score) {
            rec.setConfidenceScore(score);
            return this;
        }

        public Builder reason(String reason) {
            rec.setReason(reason);
            return this;
        }

        public Builder sourceUserIds(List<String> ids) {
            rec.setSourceUserIds(ids);
            rec.setSourceCount(ids != null ? ids.size() : 0);
            return this;
        }

        public Recommendation build() {
            return rec;
        }
    }
}
