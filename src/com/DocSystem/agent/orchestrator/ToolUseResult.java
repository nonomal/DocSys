package com.DocSystem.agent.orchestrator;

import java.util.List;
import java.util.Map;

/**
 * ToolUseLoop 执行结果。
 */
public class ToolUseResult {

    /** 是否成功（成功 = LLM 给出了最终回答；失败 = 超轮数/异常） */
    public final boolean success;

    /** LLM 最终回答（成功时）或错误信息（失败时） */
    public final String message;

    /** 完整对话转录（system + user + assistant + 工具结果），供审计/调试 */
    public final List<Map<String, String>> transcript;

    /** 实际使用的轮数（LLM 调用次数） */
    public final int turns;

    /** 工具调用总次数 */
    public final int toolCalls;

    /** 是否因超轮数被中断 */
    public final boolean maxTurnsExceeded;

    public ToolUseResult(boolean success, String message, List<Map<String, String>> transcript,
                         int turns, int toolCalls, boolean maxTurnsExceeded) {
        this.success = success;
        this.message = message;
        this.transcript = transcript;
        this.turns = turns;
        this.toolCalls = toolCalls;
        this.maxTurnsExceeded = maxTurnsExceeded;
    }

    public static ToolUseResult success(String answer, List<Map<String, String>> transcript,
                                        int turns, int toolCalls) {
        return new ToolUseResult(true, answer, transcript, turns, toolCalls, false);
    }

    public static ToolUseResult error(String message, List<Map<String, String>> transcript,
                                      int turns, int toolCalls, boolean maxTurnsExceeded) {
        return new ToolUseResult(false, message, transcript, turns, toolCalls, maxTurnsExceeded);
    }

    @Override
    public String toString() {
        return "ToolUseResult{success=" + success + ", turns=" + turns
                + ", toolCalls=" + toolCalls + ", message=" + (message != null && message.length() > 60
                ? message.substring(0, 60) + "..." : message) + "}";
    }
}
