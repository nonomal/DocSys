package com.DocSystem.agent.tool;

import com.alibaba.fastjson.JSONObject;

/**
 * LLM 输出的工具调用解析结果。
 */
public class ToolCall {

    /** 工具名 */
    public final String name;

    /** 参数（已解析 JSON；无参数时为空对象） */
    public final JSONObject arguments;

    /** 原始输出（LLM 那段包含 tool_call 标记的文本，用于回灌上下文） */
    public final String rawOutput;

    public ToolCall(String name, JSONObject arguments, String rawOutput) {
        this.name = name;
        this.arguments = arguments != null ? arguments : new JSONObject();
        this.rawOutput = rawOutput;
    }

    @Override
    public String toString() {
        return "ToolCall{name=" + name + ", arguments=" + arguments + "}";
    }
}
