package com.docsys.agent.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * SubTask - represents a decomposed subtask from the main agent
 */
public class SubTask {
    private final String id;
    private final String description;
    private final Map<String, String> params;
    
    public SubTask(String id, String description) {
        this(id, description, new HashMap<>());
    }
    
    public SubTask(String id, String description, Map<String, String> params) {
        this.id = id;
        this.description = description;
        this.params = params;
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public Map<String, String> getParams() { return params; }
    
    public void setParam(String key, String value) {
        params.put(key, value);
    }
}
