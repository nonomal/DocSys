package com.DocSystem.agent.config;

import com.DocSystem.agent.orchestrator.ToolUseLoop;
import com.DocSystem.agent.repository.AgentConfigRepository;
import com.DocSystem.agent.tool.ToolDefinition;
import com.DocSystem.agent.tool.ToolRegistry;
import com.DocSystem.agent.tool.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T8.6 护栏：AgentConfigService（fake repository）+ ToolUseLoop system prompt decorator。
 * 无 Spring、无真实 DB、无真实 LLM 依赖。
 */
public class TestAgentConfig {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        System.out.println("======== TestAgentConfig start ========");

        testServiceSetNewThenGet();
        testServiceSetExistingOverrides();
        testServiceGetMissing();
        testServiceGetFailureSilent();
        testServiceSetFailureSilent();
        testServiceList();
        testServiceGlobalHelpers();
        testServiceNullGuard();

        testDecoratorAbsentKeepsBase();
        testDecoratorOverride();
        testDecoratorSuffix();

        System.out.println("\n======== TestAgentConfig: " + pass + " passed, " + fail + " failed ========");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            pass++;
            System.out.println("  [PASS] " + name);
        } else {
            fail++;
            System.out.println("  [FAIL] " + name);
        }
    }

    // ---------- 内存版 fake repository ----------

    private static class FakeAgentConfigRepository implements AgentConfigRepository {
        final Map<String, AgentConfigEntity> store = new LinkedHashMap<>();
        boolean throwOnRead = false;
        boolean throwOnWrite = false;

        @Override
        public int insert(AgentConfigEntity entity) {
            if (throwOnWrite) throw new RuntimeException("db down");
            store.put(entity.getScope() + "|" + entity.getConfigKey(), entity);
            return 1;
        }

        @Override
        public int update(String scope, String configKey, String configValue) {
            if (throwOnWrite) throw new RuntimeException("db down");
            AgentConfigEntity e = store.get(scope + "|" + configKey);
            if (e != null) {
                e.setConfigValue(configValue);
            }
            return 1;
        }

        @Override
        public AgentConfigEntity selectByKey(String scope, String configKey) {
            if (throwOnRead) throw new RuntimeException("db down");
            return store.get(scope + "|" + configKey);
        }

        @Override
        public List<AgentConfigEntity> selectByScope(String scope) {
            if (throwOnRead) throw new RuntimeException("db down");
            List<AgentConfigEntity> out = new ArrayList<>();
            for (Map.Entry<String, AgentConfigEntity> en : store.entrySet()) {
                if (en.getKey().startsWith(scope + "|")) {
                    out.add(en.getValue());
                }
            }
            return out;
        }
    }

    private static AgentConfigService service(FakeAgentConfigRepository repo) {
        AgentConfigService svc = new AgentConfigService();
        svc.setRepository(repo);
        return svc;
    }

    // ---------- AgentConfigService ----------

    private static void testServiceSetNewThenGet() {
        FakeAgentConfigRepository repo = new FakeAgentConfigRepository();
        AgentConfigService svc = service(repo);

        boolean ok = svc.set(AgentConfigService.SCOPE_GLOBAL, AgentConfigService.KEY_SYSTEM_PROMPT_SUFFIX, "请用中文回答");
        check("set new returns true", ok);
        check("set new inserts row", repo.store.size() == 1);
        check("get returns stored value",
                "请用中文回答".equals(svc.get(AgentConfigService.SCOPE_GLOBAL, AgentConfigService.KEY_SYSTEM_PROMPT_SUFFIX)));
    }

    private static void testServiceSetExistingOverrides() {
        FakeAgentConfigRepository repo = new FakeAgentConfigRepository();
        AgentConfigService svc = service(repo);

        svc.set("global", "k1", "v1");
        boolean ok = svc.set("global", "k1", "v2");
        check("set existing returns true", ok);
        check("set existing keeps single row", repo.store.size() == 1);
        check("set existing overrides value", "v2".equals(svc.get("global", "k1")));
    }

    private static void testServiceGetMissing() {
        AgentConfigService svc = service(new FakeAgentConfigRepository());
        check("get missing returns null", svc.get("global", "nope") == null);
    }

    private static void testServiceGetFailureSilent() {
        FakeAgentConfigRepository repo = new FakeAgentConfigRepository();
        repo.throwOnRead = true;
        AgentConfigService svc = service(repo);
        check("get swallows db error -> null", svc.get("global", "k1") == null);
    }

    private static void testServiceSetFailureSilent() {
        FakeAgentConfigRepository repo = new FakeAgentConfigRepository();
        repo.throwOnWrite = true;
        AgentConfigService svc = service(repo);
        check("set swallows db error -> false", !svc.set("global", "k1", "v1"));
    }

    private static void testServiceList() {
        FakeAgentConfigRepository repo = new FakeAgentConfigRepository();
        AgentConfigService svc = service(repo);
        svc.set("global", "a", "1");
        svc.set("global", "b", "2");
        svc.set("other", "c", "3");
        Map<String, String> m = svc.list("global");
        check("list returns global keys only", m.size() == 2);
        check("list maps key->value", "1".equals(m.get("a")) && "2".equals(m.get("b")));
    }

    private static void testServiceGlobalHelpers() {
        AgentConfigService svc = service(new FakeAgentConfigRepository());
        boolean ok = svc.setGlobal(AgentConfigService.KEY_SYSTEM_PROMPT_SUFFIX, "suffix!");
        check("setGlobal returns true", ok);
        check("getGlobal reads global scope", "suffix!".equals(svc.getGlobal(AgentConfigService.KEY_SYSTEM_PROMPT_SUFFIX)));
    }

    private static void testServiceNullGuard() {
        AgentConfigService svc = service(new FakeAgentConfigRepository());
        check("get with null scope -> null", svc.get(null, "k") == null);
        check("set with null key -> false", !svc.set("global", null, "v"));
        check("list with null scope -> empty", svc.list(null).isEmpty());
    }

    // ---------- ToolUseLoop system prompt decorator ----------

    private static ToolRegistry buildRegistry() {
        ToolRegistry reg = new ToolRegistry();
        reg.register(ToolDefinition.builder("get_repos", "获取仓库", args -> ToolResult.ok("仓库1")).build());
        return reg;
    }

    /** 记录每轮消息列表的假 LLM */
    private static ToolUseLoop.LlmCaller recording(String finalAnswer, List<List<Map<String, String>>> seen) {
        return new ToolUseLoop.LlmCaller() {
            @Override
            public String chat(List<Map<String, String>> messages) {
                seen.add(new ArrayList<>(messages));
                return finalAnswer;
            }
        };
    }

    private static String systemOf(List<List<Map<String, String>>> seen) {
        Map<String, String> sys = seen.get(0).get(0);
        return sys.get("content");
    }

    private static void testDecoratorAbsentKeepsBase() {
        List<List<Map<String, String>>> seen = new ArrayList<>();
        ToolUseLoop loop = new ToolUseLoop(recording("好的", seen), buildRegistry(), false);
        loop.run("你好");
        String sys = systemOf(seen);
        check("no decorator -> base prompt preserved", sys != null && sys.contains("get_repos"));
    }

    private static void testDecoratorOverride() {
        List<List<Map<String, String>>> seen = new ArrayList<>();
        ToolUseLoop loop = new ToolUseLoop(recording("好的", seen), buildRegistry(), false);
        loop.setSystemPromptDecorator(base -> "完全替换的提示词");
        loop.run("你好");
        String sys = systemOf(seen);
        check("override decorator replaces base", "完全替换的提示词".equals(sys));
    }

    private static void testDecoratorSuffix() {
        List<List<Map<String, String>>> seen = new ArrayList<>();
        ToolUseLoop loop = new ToolUseLoop(recording("好的", seen), buildRegistry(), false);
        loop.setSystemPromptDecorator(base -> base + "\n\n" + "请用中文回答，先结论后细节");
        loop.run("你好");
        String sys = systemOf(seen);
        check("suffix decorator appends", sys != null && sys.endsWith("\n\n请用中文回答，先结论后细节"));
        check("suffix decorator keeps base", sys != null && sys.contains("get_repos"));
    }
}
