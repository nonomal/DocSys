package com.DocSystem.agent.tool;

/**
 * Tool execution result.
 *
 * <p>携带给 LLM 回灌的文本摘要（{@link #summary}）与结构化数据（{@link #data}）。
 * 文本摘要用于注入对话上下文；结构化数据供上层（审计/指标）使用。</p>
 */
public class ToolResult {

    /** 是否成功执行 */
    public final boolean success;

    /** 注入 LLM 上下文的文本结果（工具执行摘要） */
    public final String summary;

    /** 结构化结果（JSON 对象或 null） */
    public final Object data;

    /** 错误信息（success=false 时使用） */
    public final String error;

    private ToolResult(boolean success, String summary, Object data, String error) {
        this.success = success;
        this.summary = summary;
        this.data = data;
        this.error = error;
    }

    public static ToolResult ok(String summary) {
        return new ToolResult(true, summary, null, null);
    }

    public static ToolResult ok(String summary, Object data) {
        return new ToolResult(true, summary, data, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, null, null, message);
    }

    @Override
    public String toString() {
        if (success) {
            return summary != null ? summary : "(empty result)";
        }
        return "ERROR: " + error;
    }
}
