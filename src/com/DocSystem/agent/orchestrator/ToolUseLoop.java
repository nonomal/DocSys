package com.DocSystem.agent.orchestrator;

import com.DocSystem.agent.llm.LLMService;
import com.DocSystem.agent.llm.ResolvedLlmConfig;
import com.DocSystem.agent.llm.StreamChunk;
import com.DocSystem.agent.tool.ToolCall;
import com.DocSystem.agent.tool.ToolCallParser;
import com.DocSystem.agent.tool.ToolPromptBuilder;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private final StreamingLlmCaller streamingLlmCaller;
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
     * 流式 LLM 调用抽象（T7.1.3）—— 默认包装 {@link LLMService#streamChatChunks(List, ResolvedLlmConfig)}。
     * 返回的分片迭代器逐 token 到达（text/reasoning 分离），末尾必有 done。
     */
    @FunctionalInterface
    public interface StreamingLlmCaller {
        Iterator<StreamChunk> chat(List<Map<String, String>> messages) throws Exception;
    }

    /**
     * 流式事件回调（T7.1.3）—— SSE 路径把工具推理过程实时推给前端：
     * <ul>
     *   <li>{@link #onReasoning}：思考过程分片（灰色小字展示）</li>
     *   <li>{@link #onText}：正文分片（含工具调用中间文本，前端按需收进处理容器）</li>
     *   <li>{@link #onToolCall}：工具开始执行（工具卡片"调用中"）</li>
     *   <li>{@link #onToolResult}：工具执行完成（卡片结果/失败）</li>
     *   <li>{@link #onRetry}：本请求失败重试开始（前端清空当前消息已流式内容）</li>
     * </ul>
     * 全部默认空实现，非流式路径不受影响。
     */
    public interface StreamSink {
        default void onReasoning(String chunk) {}
        default void onText(String chunk) {}
        default void onToolCall(ToolCall call) {}
        default void onToolResult(ToolCall call, ToolResult result) {}
        default void onRetry() {}
    }

    /**
     * 便捷工厂：绑定 LLMService + resolved 配置。
     */
    public static ToolUseLoop forLlmService(LLMService llm, ToolRegistry registry,
                                             ResolvedLlmConfig resolved, boolean isAdmin) {
        return new ToolUseLoop(
                (LlmCaller) (messages -> llm.chat(messages, resolved)), registry, isAdmin);
    }

    /**
     * 便捷工厂（T7.1.3）：绑定 LLMService + resolved 配置，流式 + 非流式双通道。
     * 流式失败时（如 LLM 端点不支持 stream）可回退非流式通道，保证可用性。
     */
    public static ToolUseLoop forLlmServiceStreaming(LLMService llm, ToolRegistry registry,
                                                      ResolvedLlmConfig resolved, boolean isAdmin) {
        return new ToolUseLoop(
                (LlmCaller) (messages -> llm.chat(messages, resolved)),
                (StreamingLlmCaller) (messages -> llm.streamChatChunks(messages, resolved)),
                registry, isAdmin);
    }

    public ToolUseLoop(LlmCaller llmCaller, ToolRegistry toolRegistry, boolean isAdmin) {
        this(llmCaller, null, toolRegistry, isAdmin);
    }

    public ToolUseLoop(LlmCaller llmCaller, StreamingLlmCaller streamingLlmCaller,
                       ToolRegistry toolRegistry, boolean isAdmin) {
        this.llmCaller = llmCaller;
        this.streamingLlmCaller = streamingLlmCaller;
        this.toolRegistry = toolRegistry;
        this.isAdmin = isAdmin;
    }

    /** T8.5 每步工具审计回调（可为 null → 不审计） */
    private com.DocSystem.agent.audit.StepAuditSink stepAuditSink;

    /** T8.5 注入 Step 审计回调（工具链每步落库/观测） */
    public void setStepAuditSink(com.DocSystem.agent.audit.StepAuditSink sink) {
        this.stepAuditSink = sink;
    }

    /** 仅流式通道（测试/纯流式场景用），非流式通道为 null */
    public ToolUseLoop(StreamingLlmCaller streamingLlmCaller, ToolRegistry toolRegistry, boolean isAdmin) {
        this(null, streamingLlmCaller, toolRegistry, isAdmin);
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
        return runInternal(userQuery, priorHistory, null);
    }

    /**
     * 流式运行工具推理循环（T7.1.3）：与 {@link #run(String, List)} 完全同逻辑，
     * 只是每轮 LLM 输出逐分片回调 {@link StreamSink}（reasoning/text 实时推送，
     * 工具执行前后回调 tool_call/tool_result）。
     *
     * <p>若本实例没有流式通道（streamingLlmCaller == null），自动回退非流式执行
     * （过程事件仍回调：每轮完整响应解析后补发 text；工具事件照常）。</p>
     *
     * @param userQuery      用户请求
     * @param priorHistory   历史消息；null/空 = 新会话
     * @param sink           流式事件回调（可为 null → 退化为非流式行为）
     * @return 执行结果
     */
    public ToolUseResult runStreaming(String userQuery, List<Map<String, String>> priorHistory,
                                      StreamSink sink) {
        return runInternal(userQuery, priorHistory, sink);
    }

    /**
     * 单轮 LLM 输出执行器 —— 非流式直接返回完整响应；流式逐分片回调并聚合完整响应。
     */
    @FunctionalInterface
    private interface TurnRunner {
        String run(List<Map<String, String>> messages) throws Exception;
    }

    private ToolUseResult runInternal(String userQuery, List<Map<String, String>> priorHistory,
                                      StreamSink sink) {
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

        // 选择单轮执行器：有 sink 且流式通道可用 → 流式；否则非流式
        final TurnRunner turnRunner;
        if (sink != null && streamingLlmCaller != null) {
            turnRunner = msgs -> runStreamingTurn(msgs, sink);
        } else {
            turnRunner = msgs -> llmCaller.chat(msgs);
            if (sink != null) {
                // 流式通道不可用 → 退化为非流式（过程事件在轮末补发）
                log.warn("ToolUseLoop: stream sink provided but streaming channel unavailable, " +
                        "falling back to non-streaming turn execution");
            }
        }

        try {
            while (turns < MAX_TURNS) {
                turns++;
                String response = turnRunner.run(messages);
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
                        ToolResult hintError = ToolResult.error(
                                "你已连续多次调用相同工具/参数且结果未改变。请换一个思路："
                                + "检查参数是否正确、改用其他工具，或直接基于已有信息回答用户。");
                        messages.add(toolResultMsg(call.name, hintError));
                        if (sink != null) {
                            sink.onToolCall(call);
                            sink.onToolResult(call, hintError);
                        }
                        // T8.5：被防死循环拦截的调用也计入 step 审计（success=false）
                        if (stepAuditSink != null) {
                            stepAuditSink.onStep(turns, call, hintError, 0L);
                        }
                        consecutiveIdentical = 0;
                        lastCallKey = null;
                        continue;
                    }

                    log.info("ToolUseLoop executing tool '{}' args={}", call.name, call.arguments);
                    if (sink != null) {
                        sink.onToolCall(call);
                    }
                    // T8.5：每步工具执行计时 + 审计
                    long stepStart = System.currentTimeMillis();
                    ToolResult result = toolRegistry.execute(call.name, call.arguments, isAdmin);
                    long stepDuration = System.currentTimeMillis() - stepStart;
                    if (stepAuditSink != null) {
                        stepAuditSink.onStep(turns, call, result, stepDuration);
                    }
                    if (sink != null) {
                        sink.onToolResult(call, result);
                    }
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

    /**
     * 流式单轮执行：逐分片回调 sink（reasoning → onReasoning；text → onText），
     * 聚合完整响应文本返回（供轮末解析 tool_call）。
     */
    private String runStreamingTurn(List<Map<String, String>> messages, StreamSink sink) throws Exception {        StringBuilder full = new StringBuilder();
        Iterator<StreamChunk> it = streamingLlmCaller.chat(messages);
        while (it.hasNext()) {
            StreamChunk chunk = it.next();
            if (chunk.isDone()) break;
            if (chunk.isReasoning()) {
                sink.onReasoning(chunk.content);
            } else if (chunk.isText()) {
                full.append(chunk.content);
                sink.onText(chunk.content);
            }
        }
        return full.toString();
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
