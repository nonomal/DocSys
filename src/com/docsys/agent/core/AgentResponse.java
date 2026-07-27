package com.docsys.agent.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Response - standardized response format
 */
public class AgentResponse {
    private boolean success;
    private String message;
    private Object data;
    private List<String> toolCalls;
    private Long processingTimeMs;
    private String errorCode;
    private Map<String, Object> metadata = new HashMap<>();

    public AgentResponse() {
        this.success = true;
    }

    public static AgentResponse ok(Object data) {
        AgentResponse response = new AgentResponse();
        response.data = data;
        response.message = "Success";
        return response;
    }

    public static AgentResponse ok(String message) {
        AgentResponse response = new AgentResponse();
        response.message = message;
        return response;
    }

    public static AgentResponse error(String message) {
        AgentResponse response = new AgentResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    public static AgentResponse error(String message, String errorCode) {
        AgentResponse response = error(message);
        response.errorCode = errorCode;
        return response;
    }

    // Builder pattern
    public AgentResponse withData(Object data) {
        this.data = data;
        return this;
    }

    public AgentResponse withToolCalls(List<String> toolCalls) {
        this.toolCalls = toolCalls;
        return this;
    }

    public AgentResponse withProcessingTime(long ms) {
        this.processingTimeMs = ms;
        return this;
    }

    public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    public Map<String, Object> getMetadata() { return metadata; }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public List<String> getToolCalls() { return toolCalls; }
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public String getErrorCode() { return errorCode; }

    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(Object data) { this.data = data; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setToolCalls(List<String> toolCalls) { this.toolCalls = toolCalls; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
