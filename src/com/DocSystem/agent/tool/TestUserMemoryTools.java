package com.DocSystem.agent.tool;

import com.DocSystem.agent.client.DocSysClient;
import com.DocSystem.agent.memory.InMemoryUserMemoryStore;
import com.alibaba.fastjson.JSONObject;

/**
 * T8.3 护栏：用户记忆工具（memory_set / memory_get / memory_list）。
 * 纯 Java 自包含测试（main 入口），用内存存储 {@link InMemoryUserMemoryStore}，无 Spring/DB 依赖。
 *
 * 覆盖：
 *  - memory_set 保存 → memory_get 读回
 *  - 同 key 覆盖
 *  - 参数校验（key/value 空 → error；未登录 → error）
 *  - memory_get 不存在 → ok（提示不存在）
 *  - memory_list 全部 / 空
 *  - 用户隔离（不同 username 互不可见）
 *  - createFullRegistry 集成：memory_* 注册且可执行
 */
public class TestUserMemoryTools {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        testSetGet();
        testOverwrite();
        testValidation();
        testNotLoggedIn();
        testGetMissing();
        testListEmptyAndFull();
        testUserIsolation();
        testContentFallback();
        testRegistryIntegration();
        System.out.println("\n======== TestUserMemoryTools: " + pass + " passed, " + fail + " failed ========");
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

    private static ToolResult run(ToolDefinition tool, JSONObject args) throws Exception {
        return tool.executor.execute(args);
    }

    private static void testSetGet() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition set = DocSysToolFactory.memorySet(store, "alice");
        ToolDefinition get = DocSysToolFactory.memoryGet(store, "alice");

        JSONObject setArgs = new JSONObject();
        setArgs.put("key", "user.preferred_language");
        setArgs.put("value", "简体中文");
        ToolResult sr = run(set, setArgs);
        check("set success", sr.success && sr.summary != null && sr.summary.contains("user.preferred_language"));

        JSONObject getArgs = new JSONObject();
        getArgs.put("key", "user.preferred_language");
        ToolResult gr = run(get, getArgs);
        check("get returns value", gr.success && gr.summary.contains("简体中文"));
    }

    private static void testOverwrite() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition set = DocSysToolFactory.memorySet(store, "alice");
        ToolDefinition get = DocSysToolFactory.memoryGet(store, "alice");

        set.executor.execute(args("user.city", "北京"));
        set.executor.execute(args("user.city", "上海"));
        ToolResult gr = run(get, args("user.city", null));
        check("overwrite same key", gr.success && gr.summary.contains("上海") && !gr.summary.contains("北京"));
    }

    private static void testValidation() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition set = DocSysToolFactory.memorySet(store, "alice");

        // 完全空参数 → error
        check("set no args -> error", !run(set, new JSONObject()).success);
        // key 空且无 value/content → error
        JSONObject bad = new JSONObject();
        bad.put("key", "  ");
        check("set empty key & no value -> error", !run(set, bad).success);
        // 有 key 无 value → error
        JSONObject noValue = new JSONObject();
        noValue.put("key", "k");
        check("set empty value -> error", !run(set, noValue).success);
    }

    /** T8.3.1 容错：模型偶发用 content 传内容 / 缺 key → 自动映射 user.preference */
    private static void testContentFallback() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition set = DocSysToolFactory.memorySet(store, "alice");
        ToolDefinition get = DocSysToolFactory.memoryGet(store, "alice");

        // 场景 1：只传 content（模型真实出现的偏差）→ 自动映射 key=user.preference 保存成功
        JSONObject onlyContent = new JSONObject();
        onlyContent.put("content", "用户偏好：喜欢简洁的中文回答");
        ToolResult r1 = run(set, onlyContent);
        check("content-only -> success with auto key", r1.success && r1.summary.contains("user.preference"));
        ToolResult g1 = run(get, args("user.preference", null));
        check("content-only -> value readable", g1.success && g1.summary.contains("喜欢简洁"));

        // 场景 2：有 value 无 key → 自动映射 key=user.preference（覆盖）
        JSONObject valueOnly = new JSONObject();
        valueOnly.put("value", "主要做数据库运维");
        ToolResult r2 = run(set, valueOnly);
        check("value-only -> success with auto key", r2.success && r2.summary.contains("user.preference"));
        ToolResult g2 = run(get, args("user.preference", null));
        check("value-only -> overwrote value", g2.success && g2.summary.contains("数据库运维"));
    }

    private static void testNotLoggedIn() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition set = DocSysToolFactory.memorySet(store, null);
        ToolDefinition get = DocSysToolFactory.memoryGet(store, null);
        ToolDefinition list = DocSysToolFactory.memoryList(store, null);

        check("set not-logged-in -> error", !run(set, args("k", "v")).success);
        check("get not-logged-in -> error", !run(get, args("k", null)).success);
        check("list not-logged-in -> error", !run(list, null).success);
    }

    private static void testGetMissing() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition get = DocSysToolFactory.memoryGet(store, "alice");
        ToolResult gr = run(get, args("user.nonexistent", null));
        check("get missing -> ok with hint", gr.success && gr.summary.contains("不存在"));
    }

    private static void testListEmptyAndFull() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        ToolDefinition list = DocSysToolFactory.memoryList(store, "alice");

        ToolResult empty = run(list, null);
        check("list empty -> ok with hint", empty.success && empty.summary.contains("暂无"));

        DocSysToolFactory.memorySet(store, "alice").executor.execute(args("user.a", "1"));
        DocSysToolFactory.memorySet(store, "alice").executor.execute(args("user.b", "2"));
        ToolResult full = run(list, null);
        check("list returns all entries", full.success
                && full.summary.contains("user.a") && full.summary.contains("user.b"));
    }

    private static void testUserIsolation() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        DocSysToolFactory.memorySet(store, "alice").executor.execute(args("user.city", "北京"));

        ToolDefinition getBob = DocSysToolFactory.memoryGet(store, "bob");
        ToolResult bob = run(getBob, args("user.city", null));
        check("user isolation: bob cannot see alice's memory", bob.success && bob.summary.contains("不存在"));

        ToolDefinition listBob = DocSysToolFactory.memoryList(store, "bob");
        ToolResult bobList = run(listBob, null);
        check("user isolation: bob list empty", bobList.success && bobList.summary.contains("暂无"));
    }

    private static void testRegistryIntegration() throws Exception {
        InMemoryUserMemoryStore store = new InMemoryUserMemoryStore();
        // 用 dummy client（只读工具不执行，仅验证 memory 工具注册与执行）
        DocSysClient client = new DocSysClient("http://localhost:9999/");
        ToolRegistry reg = DocSysToolFactory.createFullRegistry(client, store, "alice");

        check("memory_set registered", reg.find("memory_set") != null);
        check("memory_get registered", reg.find("memory_get") != null);
        check("memory_list registered", reg.find("memory_list") != null);
        check("memory_set is write", reg.find("memory_set").isWrite);
        check("memory_set no confirm needed (T8.3 决策)", !reg.find("memory_set").needsConfirm);

        // 经 registry 执行 memory_set / memory_get
        ToolResult sr = reg.execute("memory_set", args("user.topic", "数据库运维"));
        check("registry execute memory_set", sr.success);
        ToolResult gr = reg.execute("memory_get", args("user.topic", null));
        check("registry execute memory_get", gr.success && gr.summary.contains("数据库运维"));

        // 无 store 时不注册 memory 工具
        ToolRegistry regNoMem = DocSysToolFactory.createFullRegistry(client);
        check("memory tools absent when store is null", regNoMem.find("memory_set") == null);
    }

    private static JSONObject args(String key, String value) {
        JSONObject o = new JSONObject();
        if (key != null) {
            o.put("key", key);
        }
        if (value != null) {
            o.put("value", value);
        }
        return o;
    }
}
