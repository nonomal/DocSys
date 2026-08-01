package com.DocSystem.agent.audit;

import com.DocSystem.agent.tool.ToolCall;
import com.DocSystem.agent.tool.ToolResult;

/**
 * Step 审计回调（T8.5）—— ToolUseLoop 每次工具调用后回调，供上层落库/观测。
 *
 * <p>ToolUseLoop 为纯 Java（无 Spring），通过本接口把"每步执行审计"交给
 * 上层（MainAgent 构造闭包 → StepAuditService 落库）。可为 null（不审计）。</p>
 */
public interface StepAuditSink {

    /**
     * 一次工具调用完成后的回调。
     *
     * @param turn       当前轮次（从 1 开始）
     * @param call       工具调用（name + arguments）
     * @param result     执行结果（success/summary/error）
     * @param durationMs 本次工具执行耗时（毫秒）
     */
    void onStep(int turn, ToolCall call, ToolResult result, long durationMs);
}
