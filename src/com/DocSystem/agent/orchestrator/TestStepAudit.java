package com.DocSystem.agent.orchestrator;

import com.DocSystem.agent.audit.StepAuditSink;
import com.DocSystem.agent.tool.ToolCall;
import com.DocSystem.agent.tool.ToolDefinition;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T8.5 护栏：Step 审计（工具链每步）—— ToolUseLoop 的 stepAuditSink 回调正确性。
 * 纯 Java 自包含测试（main 入口），用 fake LlmCaller + 内存 StepAuditSink，无 Spring/DB。
 *
 * <p>存储层说明：T8.5.1 决策（2026-08-01，用户）——step 审计只用于排查，不落库，
 * 用 DocSys 自带 Log 接口打结构化日志（docsys.log 可下载 grep）。故此处只验证
 * ToolUseLoop 正确触发回调（turn/tool/result/duration），日志输出由 MainAgent 负责。</p>
 *
 * 覆盖：
 *  - 每步工具执行回调（turn/tool/success/duration）
 *  - 防死循环 hint 拦截也计入 step 审计（success=false）
 *  - sink 为 null 时不影响循环（无 NPE）
 */
public class TestStepAudit {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        testLoopStepCallback();
        testHintStepCallback();
        testNullSinkSafe();
        System.out.println("\n======== TestStepAudit: " + pass + " passed, " + fail + " failed ========");
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

    /** 内存 StepAuditSink：收集 onStep 调用 */
    private static class CollectingSink implements StepAuditSink {
        final List<Step> steps = new ArrayList<>();
        static class Step {
            final int turn; final String tool; final boolean success; final long duration;
            Step(int turn, String tool, boolean success, long duration) {
                this.turn = turn; this.tool = tool; this.success = success; this.duration = duration;
            }
        }
        public void onStep(int turn, ToolCall call, ToolResult result, long durationMs) {
            steps.add(new Step(turn, call.name, result.success, durationMs));
        }
    }

    private static ToolUseLoop.LlmCaller scripted(List<String> script) {
        return new ToolUseLoop.LlmCaller() {
            private int idx = 0;
            public String chat(List<Map<String, String>> messages) {
                return script.get(Math.min(idx++, script.size() - 1));
            }
        };
    }

    private static ToolRegistry buildRegistry() {
        ToolRegistry reg = new ToolRegistry();
        reg.register(ToolDefinition.builder("tool_a", "测试工具A",
                args -> ToolResult.ok("A 的结果")).build());
        reg.register(ToolDefinition.builder("tool_b", "测试工具B",
                args -> ToolResult.error("B 失败")).build());
        return reg;
    }

    /** 场景：一轮调 tool_a（成功）→ 最终回答。断言 step 回调 1 次，字段正确。 */
    private static void testLoopStepCallback() {
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{\"name\":\"tool_a\",\"arguments\":{\"x\":1}}</tool_call>");
        script.add("这是最终回答。");
        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(), false);
        CollectingSink sink = new CollectingSink();
        loop.setStepAuditSink(sink);

        ToolUseResult r = loop.run("请用工具");
        check("loop success", r.success);
        check("one step audited", sink.steps.size() == 1);
        if (sink.steps.size() == 1) {
            CollectingSink.Step s = sink.steps.get(0);
            check("step turn=1", s.turn == 1);
            check("step tool=tool_a", "tool_a".equals(s.tool));
            check("step success=true", s.success);
            check("step duration>=0", s.duration >= 0);
        }
    }

    /** 场景：连续 3 次相同调用被 hint 拦截 → 计入 step 审计（success=false） */
    private static void testHintStepCallback() {
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{\"name\":\"tool_a\",\"arguments\":{}}</tool_call>");
        script.add("<tool_call>{\"name\":\"tool_a\",\"arguments\":{}}</tool_call>");
        script.add("<tool_call>{\"name\":\"tool_a\",\"arguments\":{}}</tool_call>");
        script.add("最终回答。");
        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(), false);
        CollectingSink sink = new CollectingSink();
        loop.setStepAuditSink(sink);
        ToolUseResult r = loop.run("重复调用");
        boolean hintStep = false;
        for (CollectingSink.Step s : sink.steps) {
            if (s.tool.equals("tool_a") && !s.success) hintStep = true;
        }
        check("hint step recorded as failed", hintStep);
        check("total steps recorded", sink.steps.size() >= 3);
    }

    /** 不设 sink（null）→ 循环正常，无 NPE */
    private static void testNullSinkSafe() {
        List<String> script = new ArrayList<>();
        script.add("<tool_call>{\"name\":\"tool_a\",\"arguments\":{}}</tool_call>");
        script.add("最终回答。");
        ToolUseLoop loop = new ToolUseLoop(scripted(script), buildRegistry(), false);
        try {
            ToolUseResult r = loop.run("无 sink 测试");
            check("null sink -> loop runs fine", r.success);
        } catch (Exception e) {
            check("null sink -> loop runs fine", false);
        }
    }
}
