package com.DocSystem.agent.orchestrator;

import com.DocSystem.agent.tool.ToolDefinition;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T2.1 护栏：ToolUseLoop 循环逻辑（用脚本化假 LLM 驱动，无 Spring/无真实 LLM 依赖）。
 *
 * 覆盖：
 *  - 多轮：LLM 调工具 → 拿结果 → 再调 → 最终回答（turns/toolCalls 正确）
 *  - 单轮：无工具调用 → 直接最终回答
 *  - 未知工具：registry 返回 error → 回灌继续
 *  - 畸形 tool_call：回灌重试提示 → 最终成功
 *  - 连续畸形超过上限 → 终止
 *  - 超 MAX_TURNS → error + maxTurnsExceeded
 *  - adminOnly 工具对非管理员不可见（prompt 不含该工具）
 */
public class TestToolUseLoop {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        testMultiTurnChain();
        testSingleTurnNoTool();
        testUnknownToolRecovery();
        testMalformedRetryThenSuccess();
        testTooManyMalformedAbort();
        testMaxTurnsExceeded();
        testAdminFilteringInPrompt();
        testConsecutiveIdenticalToolCall();
        testTranscriptTrimming();
        testHistoryInjection();
        System.out.println("\n======== TestToolUseLoop: " + pass + " passed, " + fail + " failed ========");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            pass++;
            System.out.println("[PASS] " + name);
        } else {
            fail++;
            System.out.println("[FAIL] " + name);
        }
    }

    // ---------- 测试工具 ----------

    private static ToolRegistry buildRegistry(boolean includeAdmin) {
        ToolRegistry reg = new ToolRegistry();
        reg.register(ToolDefinition.builder("list_repos", "列出仓库", args -> ToolResult.ok("[仓库1, 仓库2]")).build());
        reg.register(ToolDefinition.builder("get_repos", "获取仓库", args -> ToolResult.ok("仓库" + args.getString("vid") + "详情")).build());
        reg.register(ToolDefinition.builder("boom", "抛异常工具", args -> ToolResult.error("执行失败: disk full")).build());
        if (includeAdmin) {
            reg.register(ToolDefinition.builder("doc_sys_init", "系统初始化", args -> ToolResult.ok("init")).adminOnly(true).build());
        }
        return reg;
    }

    /** 脚本化假 LLM：按调用次数返回脚本中的内容 */
    private static ToolUseLoop.LlmCaller scripted(List<String> script) {
        return new ToolUseLoop.LlmCaller() {
            private int idx = 0;
            @Override
            public String chat(List<Map<String, String>> messages) {
                return script.get(Math.min(idx++, script.size() - 1));
            }
        };
    }

    /** 记录每次调用时消息列表的假 LLM（用于断言 prompt 内容） */
    private static ToolUseLoop.LlmCaller recording(List<String> script, List<List<Map<String, String>>> seen) {
        return new ToolUseLoop.LlmCaller() {
            private int idx = 0;
            @Override
            public String chat(List<Map<String, String>> messages) {
                seen.add(new ArrayList<>(messages));
                return script.get(Math.min(idx++, script.size() - 1));
            }
        };
    }

    // ---------- 测试用例 ----------

    private static void testMultiTurnChain() {
        // turn1: list_repos → turn2: get_repos → turn3: final answer
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>");
        script.add("<tool_call>{\"name\":\"get_repos\",\"arguments\":{\"vid\":1}}</tool_call>");
        script.add("共有 2 个仓库，第一个是仓库1。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("列出仓库并查看仓库1");

        check("multi-turn: success", r.success);
        check("multi-turn: turns=3", r.turns == 3);
        check("multi-turn: toolCalls=2", r.toolCalls == 2);
        check("multi-turn: final answer", r.message != null && r.message.contains("仓库1"));
        // transcript 应包含工具结果标记
        boolean hasToolResult = false;
        for (Map<String, String> m : r.transcript) {
            if (m.get("content") != null && m.get("content").contains("[TOOL_RESULT tool=list_repos]")) {
                hasToolResult = true;
                break;
            }
        }
        check("multi-turn: tool result fed back", hasToolResult);
    }

    private static void testSingleTurnNoTool() {
        List<String> script = new ArrayList<>();
        script.add("你好，我是 DocSys 的助手。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("你好");

        check("single-turn: success", r.success);
        check("single-turn: turns=1", r.turns == 1);
        check("single-turn: toolCalls=0", r.toolCalls == 0);
        check("single-turn: answer", r.message.contains("助手"));
    }

    private static void testUnknownToolRecovery() {
        // turn1: 调不存在的工具 → registry 返回 error → turn2: 最终回答
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{\"name\":\"no_such_tool\",\"arguments\":{}}</tool_call>");
        script.add("抱歉，我没有找到该功能。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("调用不存在的工具");

        check("unknown tool: success (recovered)", r.success);
        check("unknown tool: turns=2", r.turns == 2);
        check("unknown tool: toolCalls=1", r.toolCalls == 1);
        // transcript 含错误回灌
        boolean hasErr = false;
        for (Map<String, String> m : r.transcript) {
            if (m.get("content") != null && m.get("content").contains("Unknown tool")) {
                hasErr = true;
                break;
            }
        }
        check("unknown tool: error fed back", hasErr);
    }

    private static void testMalformedRetryThenSuccess() {
        // turn1: 畸形 → turn2: 正常工具调用 → turn3: 最终回答
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{bad json}</tool_call>");
        script.add("<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>");
        script.add("仓库列表如上。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("查仓库");

        check("malformed retry: success", r.success);
        check("malformed retry: turns=3", r.turns == 3);
        check("malformed retry: toolCalls=1", r.toolCalls == 1);
    }

    private static void testTooManyMalformedAbort() {
        List<String> script = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            script.add("<tool_call>{bad json}</tool_call>");
        }

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("查仓库");

        check("too many malformed: not success", !r.success);
        check("too many malformed: aborted", r.message != null && r.message.contains("格式错误"));
    }

    private static void testMaxTurnsExceeded() {
        // 一直调工具不回答 → 超 MAX_TURNS
        List<String> script = new ArrayList<>();
        for (int i = 0; i < ToolUseLoop.MAX_TURNS + 2; i++) {
            script.add("<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>");
        }

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("循环调工具");

        check("max turns: not success", !r.success);
        check("max turns: flag set", r.maxTurnsExceeded);
        check("max turns: turns=" + ToolUseLoop.MAX_TURNS, r.turns == ToolUseLoop.MAX_TURNS);
        check("max turns: toolCalls=" + ToolUseLoop.MAX_TURNS, r.toolCalls == ToolUseLoop.MAX_TURNS);
    }

    private static void testAdminFilteringInPrompt() {
        List<List<Map<String, String>>> seen = new ArrayList<>();
        List<String> script = new ArrayList<>();
        script.add("好的");

        // 非管理员：prompt 不应含 doc_sys_init
        ToolUseLoop nonAdmin = new ToolUseLoop(recording(script, seen), buildRegistry(true), false);
        nonAdmin.run("hi");
        String nonAdminPrompt = systemContent(seen.get(0));
        check("non-admin prompt hides adminOnly", !nonAdminPrompt.contains("doc_sys_init"));
        check("non-admin prompt has read tools", nonAdminPrompt.contains("list_repos"));

        // 管理员：prompt 应含 doc_sys_init
        List<List<Map<String, String>>> seen2 = new ArrayList<>();
        ToolUseLoop admin = new ToolUseLoop(recording(script, seen2), buildRegistry(true), true);
        admin.run("hi");
        String adminPrompt = systemContent(seen2.get(0));
        check("admin prompt shows adminOnly", adminPrompt.contains("doc_sys_init"));
    }

    private static String systemContent(List<Map<String, String>> messages) {
        for (Map<String, String> m : messages) {
            if ("system".equals(m.get("role"))) {
                return m.get("content") != null ? m.get("content") : "";
            }
        }
        return "";
    }

    // ---------- T2.2/T2.3 增强测试 ----------

    private static void testConsecutiveIdenticalToolCall() {
        // 连续 3 次相同工具调用 → 注入提示 → 第4轮给最终回答
        List<String> script = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            script.add("<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>");
        }
        script.add("基于已有信息回答。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("反复查仓库");

        check("identical calls: success", r.success);
        // transcript 应包含"重复调用"提示
        boolean hasHint = false;
        for (Map<String, String> m : r.transcript) {
            if (m.get("content") != null && m.get("content").contains("连续多次调用相同工具")) {
                hasHint = true;
                break;
            }
        }
        check("identical calls: hint injected", hasHint);
    }

    private static void testHistoryInjection() {
        // 历史上下文注入：LLM 首次调用应能看到前文
        List<List<Map<String, String>>> seen = new ArrayList<>();
        List<String> script = new ArrayList<>();
        script.add("好的，我记得前面的对话了。");

        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> h1 = new java.util.HashMap<>();
        h1.put("role", "user");
        h1.put("content", "我叫小明");
        Map<String, String> h2 = new java.util.HashMap<>();
        h2.put("role", "assistant");
        h2.put("content", "你好小明");
        history.add(h1);
        history.add(h2);

        ToolUseLoop loop = new ToolUseLoop(recording(script, seen), buildRegistry(false), false);
        ToolUseResult r = loop.run("我姓什么？", history);

        check("history: success", r.success);
        // 首次调用消息列表应包含历史
        List<Map<String, String>> firstMessages = seen.get(0);
        boolean hasHistory = false;
        for (Map<String, String> m : firstMessages) {
            if ("我叫小明".equals(m.get("content")) || "你好小明".equals(m.get("content"))) {
                hasHistory = true;
                break;
            }
        }
        check("history: injected into messages", hasHistory);
        check("history: system first", "system".equals(firstMessages.get(0).get("role")));
        check("history: user query last", "user".equals(firstMessages.get(firstMessages.size() - 1).get("role"))
                && "我姓什么？".equals(firstMessages.get(firstMessages.size() - 1).get("content")));
    }

    private static void testTranscriptTrimming() {
        // 每轮 3 个不同工具调用（避开 identical 检测），8 轮后消息数 2+8*4=34 → 触发裁剪到 ≤30，
        // 第 9 轮给出最终回答（未超 MAX_TURNS=10）。
        List<String> script = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String turn = "<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>"
                    + "<tool_call>{\"name\":\"get_repos\",\"arguments\":{\"vid\":" + (i * 2) + "}}</tool_call>"
                    + "<tool_call>{\"name\":\"get_repos\",\"arguments\":{\"vid\":" + (i * 2 + 1) + "}}</tool_call>";
            script.add(turn);
        }
        script.add("完成。");

        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(false), false);
        ToolUseResult r = loop.run("连续查询");

        check("trimming: success", r.success);
        check("trimming: turns=9", r.turns == 9);
        check("trimming: toolCalls=24", r.toolCalls == 24);
        check("trimming: not maxTurnsExceeded", !r.maxTurnsExceeded);
        // transcript 不应无限膨胀（裁剪后 ≤ 31）
        check("trimming: transcript capped", r.transcript.size() <= 31);
    }
}
