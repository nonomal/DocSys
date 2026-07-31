package com.DocSystem.agent.orchestrator;

import com.DocSystem.agent.llm.LLMService;
import com.DocSystem.agent.llm.ResolvedLlmConfig;
import com.DocSystem.agent.tool.ToolCall;
import com.DocSystem.agent.tool.ToolCallParser;
import com.DocSystem.agent.tool.ToolPromptBuilder;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ToolUseLoop —— 工具推理循环（REPL）：LLM 思考 → 决定调工具 → 执行 → 观察结果 → 再思考 → …直到给出最终回答。
 *
 * <p>这是把 Agent 从"意图分类器"升级为"推理引擎"的核心引擎，对应开发计划 T2.1。</p>
 *
 * <p>流程：</p>
 * <pre>
 * messages = [system(ToolPromptBuilder 渲染), user(userQuery)]
 * loop (MAX_TURNS) {
 *   response = llm.chat(messages, resolvedLlm)
 *   calls = ToolCallParser.parse(response)
 *   if (calls == null)            → 回灌"tool_call 格式错误"消息，继续（连续错误计数防死循环）
 *   if (calls.isEmpty())          → response 即最终回答，返回 success
 *   messages += assistant(response)        // LLM 的工具调用原文
 *   for each call:
 *     result = registry.execute(call)
 *     messages += toolResult(call, result) // 工具结果回灌
 * }
 * → 超轮数，返回 error
 * </pre>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>通过 {@link LlmCaller} 抽象 LLM 调用（默认包装 LLMService，测试可注入假实现，无需 Spring）。</li>
 *   <li>消息列表由本类自行管理（不用 LLMService.conversationHistory），以便自定义 system 提示词。</li>
 *   <li>工具结果以 role=user + {@code [TOOL_RESULT name=...]} 标记回灌（兼容不支持 tool role 的模型）。</li>
 *   <li>所有请求级配置为局部变量，不写共享字段（线程安全）。</li>
 * </ul>
 */
public class ToolUseLoop {

    private static final Logger log = LoggerFactory.getLogger(ToolUseLoop.class);

    /** 单次请求最大轮数（LLM 调用次数硬上限） */
    public static final int MAX_TURNS = 10;

    /** 连续 tool_call 格式错误最大次数（超过则放弃，防死循环） */
    private static final int MAX_MALFORMED = 3;

    /** 连续相同工具调用最大次数（超过则注入提示，防死循环） */
    private static final int MAX_IDENTICAL_CALLS = 3;

    /** 对话转录最大消息条数（超出裁剪最早的工具结果，防上下文膨胀） */
    private static final int MAX_TRANSCRIPT_SIZE = 30;

    private final LlmCaller llmCaller;
    private final ToolRegistry toolRegistry;

    /** 是否管理员（决定 adminOnly 工具可见性） */
    private final boolean isAdmin;

    /**
     * LLM 调用抽象 —— 默认包装 {@link LLMService#chat(List, ResolvedLlmConfig)}。
     */
    @FunctionalInterface
    public interface LlmCaller {
        String chat(List<Map<String, String>> messages) throws Exception;
    }

    /**
     * 便捷工厂：绑定 LLMService + resolved 配置。
     */
    public static ToolUseLoop forLlmService(LLMService llm, ToolRegistry registry,
                                             ResolvedLlmConfig resolved, boolean isAdmin) {
        return new ToolUseLoop(messages -> llm.chat(messages, resolved), registry, isAdmin);
    }

    public ToolUseLoop(LlmCaller llmCaller, ToolRegistry toolRegistry, boolean isAdmin) {
        this.llmCaller = llmCaller;
        this.toolRegistry = toolRegistry;
        this.isAdmin = isAdmin;
    }

    /**
     * 运行工具推理循环。
     *
     * @param userQuery 用户请求
     * @return 执行结果（成功 = 最终回答；失败 = 超轮数/异常）
     */
    public ToolUseResult run(String userQuery) {
        return run(userQuery, null);
    }

    /**
     * 运行工具推理循环（带历史上下文，T5.2b 会话记忆）。
     *
     * @param userQuery      用户请求
     * @param priorHistory   历史消息（role ∈ user/assistant，按时间顺序）；null/空 = 新会话
     * @return 执行结果
     */
    public ToolUseResult run(String userQuery, List<Map<String, String>> priorHistory) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMsg(ToolPromptBuilder.buildSystemPrompt(toolRegistry.listForUser(isAdmin))));
        // 历史上下文注入（续接会话时 LLM 记得前文）
        if (priorHistory != null) {
            for (Map<String, String> h : priorHistory) {
                String role = h.get("role");
                String content = h.get("content");
                if (content == null || content.isEmpty()) continue;
                if ("user".equals(role) || "assistant".equals(role)) {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", role);
                    m.put("content", content);
                    messages.add(m);
                }
            }
        }
        messages.add(userMsg(userQuery));

        int turns = 0;
        int toolCalls = 0;
        int consecutiveMalformed = 0;
        String lastCallKey = null;        // 上一个工具调用签名（name+args），用于重复检测
        int consecutiveIdentical = 0;     // 连续相同工具调用计数

        try {
            while (turns < MAX_TURNS) {
                turns++;
                String response = llmCaller.chat(messages);
                log.debug("ToolUseLoop turn {}: response={}", turns, truncate(response));

                List<ToolCall> calls = ToolCallParser.parse(response);
                if (calls == null) {
                    // tool_call 标记存在但格式错误 → 回灌错误消息让 LLM 重试
                    consecutiveMalformed++;
                    if (consecutiveMalformed >= MAX_MALFORMED) {
                        log.warn("ToolUseLoop: too many malformed tool_call blocks, aborting");
                        return ToolUseResult.error(
                                "AI 多次输出格式错误的工具调用，已终止。请稍后重试或换个说法。",
                                messages, turns, toolCalls, false);
                    }
                    messages.add(assistantMsg(response));
                    messages.add(userMsg(
                            "[SYSTEM] 你的工具调用格式无效。请严格按以下格式输出（JSON 必须合法）：\n" +
                            "<tool_call>{\"name\":\"工具名\",\"arguments\":{...}}</tool_call>"));
                    continue;
                }

                if (calls.isEmpty()) {
                    // 无工具调用 → 最终回答
                    return ToolUseResult.success(response, messages, turns, toolCalls);
                }

                // 有工具调用 → 执行并回灌
                consecutiveMalformed = 0;
                messages.add(assistantMsg(response));
                for (ToolCall call : calls) {
                    toolCalls++;

                    // 连续相同工具调用检测（防死循环：LLM 反复调同一工具不换招）
                    String callKey = call.name + "|" + (call.arguments != null ? call.arguments.toJSONString() : "{}");
                    if (callKey.equals(lastCallKey)) {
                        consecutiveIdentical++;
                    } else {
                        consecutiveIdentical = 1;
                        lastCallKey = callKey;
                    }
                    if (consecutiveIdentical >= MAX_IDENTICAL_CALLS) {
                        log.warn("ToolUseLoop: tool '{}' called {} times identically, injecting hint",
                                call.name, MAX_IDENTICAL_CALLS);
                        messages.add(toolResultMsg(call.name, ToolResult.error(
                                "你已连续多次调用相同工具/参数且结果未改变。请换一个思路："
                                + "检查参数是否正确、改用其他工具，或直接基于已有信息回答用户。")));
                        consecutiveIdentical = 0;
                        lastCallKey = null;
                        continue;
                    }

                    log.info("ToolUseLoop executing tool '{}' args={}", call.name, call.arguments);
                    ToolResult result = toolRegistry.execute(call.name, call.arguments, isAdmin);
                    messages.add(toolResultMsg(call.name, result));
                }
                // 上下文裁剪：保留 system + user 开头，超长时丢弃最早的工具结果
                trimTranscript(messages);
            }
        } catch (Exception e) {
            log.error("ToolUseLoop failed", e);
            return ToolUseResult.error("工具推理失败: " + e.getMessage(), messages, turns, toolCalls, false);
        }

        log.warn("ToolUseLoop exceeded MAX_TURNS={}", MAX_TURNS);
        return ToolUseResult.error(
                "处理超时：AI 连续调用工具过多仍未给出回答（已中断）。请缩小请求范围或重试。",
                messages, turns, toolCalls, true);
    }

    // ---------- 消息构造 ----------

    private static Map<String, String> systemMsg(String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "system");
        m.put("content", content);
        return m;
    }

    private static Map<String, String> userMsg(String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", content);
        return m;
    }

    private static Map<String, String> assistantMsg(String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "assistant");
        m.put("content", content);
        return m;
    }

    /** 工具结果回灌：role=user + 结构化标记（兼容不支持 tool role 的模型） */
    private static Map<String, String> toolResultMsg(String toolName, ToolResult r) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", "[TOOL_RESULT tool=" + toolName + "]\n" + r.toString() + "\n[/TOOL_RESULT]");
        return m;
    }

    /**
     * 上下文裁剪：消息总数超 MAX_TRANSCRIPT_SIZE 时，丢弃最早的工具结果消息（保留 system + user 开头）。
     */
    private static void trimTranscript(List<Map<String, String>> messages) {
        while (messages.size() > MAX_TRANSCRIPT_SIZE) {
            // 从位置 2 起找第一条 role=user 且内容为 [TOOL_RESULT 的消息删除
            boolean removed = false;
            for (int i = 2; i < messages.size(); i++) {
                Map<String, String> m = messages.get(i);
                String content = m.get("content");
                if ("user".equals(m.get("role"))
                        && content != null && content.startsWith("[TOOL_RESULT")) {
                    messages.remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                // 没有可删的工具结果 → 删位置 2（最早的中间消息）
                messages.remove(2);
            }
        }
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
