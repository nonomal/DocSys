package com.docsys.agent.skill.executor;

import java.util.Collections;
import java.util.Map;

/**
 * SkillExecutionResult - immutable result of a skill execution.
 * Never returns null — empty string/map is used instead.
 */
public final class SkillExecutionResult {
    private final boolean success;
    private final String output;
    private final String error;
    private final Map<String, Object> data;

    public SkillExecutionResult(boolean success, String output, String error, Map<String, Object> data) {
        this.success = success;
        this.output = output == null ? "" : output;
        this.error = error == null ? "" : error;
        this.data = data == null ? Collections.emptyMap() : data;
    }

    public boolean success() { return success; }
    public String output() { return output; }
    public String error() { return error; }
    public Map<String, Object> data() { return data; }

    /** Create a successful result with output and empty data. */
    public static SkillExecutionResult ok(String output) {
        return new SkillExecutionResult(true, output == null ? "" : output, "", Collections.emptyMap());
    }

    /** Create a successful result with output and extra data. */
    public static SkillExecutionResult ok(String output, Map<String, Object> data) {
        return new SkillExecutionResult(true, output == null ? "" : output, "", data == null ? Collections.emptyMap() : data);
    }

    /** Create a failed result with an error message and empty data. */
    public static SkillExecutionResult error(String error) {
        return new SkillExecutionResult(false, "", error == null ? "" : error, Collections.emptyMap());
    }

    /** Create a failed result with an error message and extra data. */
    public static SkillExecutionResult error(String error, Map<String, Object> data) {
        return new SkillExecutionResult(false, "", error == null ? "" : error, data == null ? Collections.emptyMap() : data);
    }
}
