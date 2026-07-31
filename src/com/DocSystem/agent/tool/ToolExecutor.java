package com.DocSystem.agent.tool;

import com.alibaba.fastjson.JSONObject;

/**
 * 工具执行器 —— 函数式接口。
 *
 * <p>接收已解析的 JSON 参数（LLM 的 arguments），返回 {@link ToolResult}。
 * 实现方通常闭包捕获 {@code DocSysClient}（per-request 会话隔离实例）。</p>
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行工具。
     *
     * @param args 已解析的 JSON 参数对象（与 ToolDefinition.parameters Schema 对应）
     * @return 执行结果（成功/失败 + 文本摘要 + 结构化数据）
     * @throws Exception 底层 API 异常（由 ToolRegistry 捕获包装为 ToolResult.error）
     */
    ToolResult execute(JSONObject args) throws Exception;
}
