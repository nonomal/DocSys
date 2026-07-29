package com.DocSystem.agent.nlu;

import java.util.HashMap;
import java.util.Map;

/**
 * Intent Result - structured output from LLM-powered intent recognition.
 *
 * Carries the recognized intent, sub-task type, confidence score,
 * extracted entities, and metadata about the parsing source.
 */
public class IntentResult {

    /**
     * Intent type, e.g. "list", "search", "chat".
     * Matches the output of MainAgent.extractIntent().
     */
    private String intent;

    /**
     * Sub-task type for decomposeTask routing, e.g. "list_repos", "search_and_load".
     */
    private String subTaskType;

    /**
     * Confidence score 0.0 - 1.0.
     * Lower values signal the caller should fall back to regex parsing.
     */
    private float confidence;

    /**
     * Extracted entities such as vid, docId, query, name, topic.
     */
    private Map<String, String> entities;

    /**
     * Whether this result was produced by the LLM (true) or by fallback (false).
     */
    private boolean fromLLM;

    /**
     * Original user query string.
     */
    private String rawQuery;

    /**
     * Human-readable reasoning produced by the LLM.
     */
    private String reasoning;

    public IntentResult() {
        this.entities = new HashMap<>();
        this.confidence = 0.0f;
        this.fromLLM = false;
    }

    public IntentResult(String intent, String subTaskType, float confidence,
                        Map<String, String> entities, boolean fromLLM, String rawQuery) {
        this.intent = intent;
        this.subTaskType = subTaskType;
        this.confidence = confidence;
        this.entities = entities != null ? entities : new HashMap<>();
        this.fromLLM = fromLLM;
        this.rawQuery = rawQuery;
    }

    // Factory methods

    /** Returns an empty result indicating LLM parsing was skipped. */
    public static IntentResult empty() {
        IntentResult r = new IntentResult();
        r.setFromLLM(false);
        r.setConfidence(0.0f);
        return r;
    }

    /** Creates a result for an unknown intent with low confidence. */
    public static IntentResult unknown(String rawQuery) {
        IntentResult r = new IntentResult();
        r.setIntent("unknown");
        r.setSubTaskType("chat");
        r.setConfidence(0.1f);
        r.setFromLLM(false);
        r.setRawQuery(rawQuery);
        return r;
    }

    // Getters

    public String getIntent() {
        return intent;
    }

    public String getSubTaskType() {
        return subTaskType;
    }

    public float getConfidence() {
        return confidence;
    }

    public Map<String, String> getEntities() {
        return entities;
    }

    public boolean isFromLLM() {
        return fromLLM;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public String getReasoning() {
        return reasoning;
    }

    // Setters

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setSubTaskType(String subTaskType) {
        this.subTaskType = subTaskType;
    }

    public void setConfidence(float confidence) {
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
    }

    public void setEntities(Map<String, String> entities) {
        this.entities = entities != null ? entities : new HashMap<>();
    }

    public void setFromLLM(boolean fromLLM) {
        this.fromLLM = fromLLM;
    }

    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    /**
     * Convenience getter for a single entity.
     * @return entity value or null if not present
     */
    public String getEntity(String key) {
        return entities != null ? entities.get(key) : null;
    }

    /**
     * Put a single entity.
     */
    public void putEntity(String key, String value) {
        if (entities == null) {
            entities = new HashMap<>();
        }
        entities.put(key, value);
    }

    /**
     * Convert to an immutable Map representation.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("intent", intent != null ? intent : "unknown");
        map.put("subTaskType", subTaskType != null ? subTaskType : "");
        map.put("confidence", confidence);
        map.put("entities", entities != null ? entities : new HashMap<>());
        map.put("fromLLM", fromLLM);
        map.put("rawQuery", rawQuery != null ? rawQuery : "");
        map.put("reasoning", reasoning != null ? reasoning : "");
        return map;
    }

    @Override
    public String toString() {
        return "IntentResult{" +
                "intent='" + intent + '\'' +
                ", subTaskType='" + subTaskType + '\'' +
                ", confidence=" + confidence +
                ", entities=" + entities +
                ", fromLLM=" + fromLLM +
                ", rawQuery='" + rawQuery + '\'' +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}
