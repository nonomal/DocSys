package com.DocSystem.agent.orchestrator;

import com.DocSystem.agent.audit.StepAuditEntity;
import com.DocSystem.agent.audit.StepAuditService;
import com.DocSystem.agent.audit.StepAuditSink;
import com.DocSystem.agent.repository.StepAuditRepository;
import com.DocSystem.agent.tool.ToolCall;
import com.DocSystem.agent.tool.ToolDefinition;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T8.5 护栏：Step 审计（工具链每步结构化审计）。
 * 纯 Java 自包含测试（main 入口），用 fake LlmCaller + fake StepAuditSink/Repository，无 Spring/DB。
 *
 * 覆盖：
 *  - ToolUseLoop 每步工具执行回调 stepAuditSink（turn/tool/result/duration）
 *  - 防死循环 hint 拦截也计入 step 审计（success=false）
 *  - StepAuditService.record 落库（fake repository 收到 entity 且字段正确）
 *  - 参数/结果摘要截断
 *  - 审计失败不抛（不影响主流程）
 */
public class TestStepAudit {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        testLoopStepCallback();
        testHintStepCallback();
        testServiceRecord();
        testServiceTruncation();
        testServiceFailsSilently();
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
        // 第3次相同调用被拦截 → hint step（failed）；第1、2次正常成功
        boolean hintStep = false;
        for (CollectingSink.Step s : sink.steps) {
            if (s.tool.equals("tool_a") && !s.success) hintStep = true;
        }
        check("hint step recorded as failed", hintStep);
        check("total steps recorded", sink.steps.size() >= 3);
    }

    /** 内存 StepAuditRepository（fake） */
    private static class FakeRepository implements StepAuditRepository {
        final List<StepAuditEntity> rows = new ArrayList<>();
        public int insert(StepAuditEntity entity) { rows.add(entity); return 1; }
        public List<StepAuditEntity> selectRecent(int limit) {
            return new ArrayList<>(rows);
        }
        public List<StepAuditEntity> selectByRequestId(String requestId) {
            List<StepAuditEntity> out = new ArrayList<>();
            for (StepAuditEntity e : rows) { if (requestId.equals(e.getRequestId())) out.add(e); }
            return out;
        }
        public List<StepAuditEntity> selectBySessionId(String sessionId) {
            List<StepAuditEntity> out = new ArrayList<>();
            for (StepAuditEntity e : rows) { if (sessionId.equals(e.getSessionId())) out.add(e); }
            return out;
        }
    }

    /** StepAuditService.record → fake repository 收到字段正确的 entity */
    private static void testServiceRecord() {
        StepAuditService svc = new StepAuditService();
        FakeRepository repo = new FakeRepository();
        svc.setRepository(repo);

        svc.record("req-1", "sess-1", 2, "list_docs", "{\"vid\":8}", "找到 3 个文档", true, 42L);
        check("service record -> 1 row", repo.rows.size() == 1);
        if (repo.rows.size() == 1) {
            StepAuditEntity e = repo.rows.get(0);
            check("record requestId", "req-1".equals(e.getRequestId()));
            check("record sessionId", "sess-1".equals(e.getSessionId()));
            check("record turn", e.getTurn() == 2);
            check("record tool", "list_docs".equals(e.getTool()));
            check("record args", "{\"vid\":8}".equals(e.getArgsSummary()));
            check("record result", "找到 3 个文档".equals(e.getResultSummary()));
            check("record success", Boolean.TRUE.equals(e.getSuccess()));
            check("record duration", e.getDurationMs() == 42L);
            check("record createdAt", e.getCreatedAt() != null);
        }
        // 失败工具：resultSummary 应为 error
        svc.record("req-1", "sess-1", 3, "tool_b", "{}", "B 失败", false, 10L);
        check("failed step recorded with error", repo.rows.size() == 2
                && "B 失败".equals(repo.rows.get(1).getResultSummary())
                && Boolean.FALSE.equals(repo.rows.get(1).getSuccess()));
    }

    /** 长参数/结果摘要被截断 */
    private static void testServiceTruncation() {
        StepAuditService svc = new StepAuditService();
        FakeRepository repo = new FakeRepository();
        svc.setRepository(repo);
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 600; i++) big.append('x');
        svc.record("r", "s", 1, "tool_a", big.toString(), big.toString(), true, 1L);
        StepAuditEntity e = repo.rows.get(0);
        check("args truncated", e.getArgsSummary().length() < 600 && e.getArgsSummary().endsWith("...(truncated)"));
        check("result truncated", e.getResultSummary().length() < 600 && e.getResultSummary().endsWith("...(truncated)"));
    }

    /** 审计失败不抛（repository 抛异常 → record 吞掉） */
    private static void testServiceFailsSilently() {
        StepAuditService svc = new StepAuditService();
        StepAuditRepository throwingRepo = new FakeRepository() {
            public int insert(StepAuditEntity entity) { throw new RuntimeException("db down"); }
        };
        svc.setRepository(throwingRepo);
        try {
            svc.record("r", "s", 1, "tool_a", "{}", "ok", true, 1L);
            check("audit failure swallowed (no throw)", true);
        } catch (Exception e) {
            check("audit failure swallowed (no throw)", false);
        }
    }
}
