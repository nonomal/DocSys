package com.DocSystem.agent.tool;

import java.util.List;

/**
 * T1.6 护栏：ToolCallParser。
 * 纯 Java 自包含测试（main 入口），无 Spring 依赖。
 *
 * 覆盖：
 *  - 正确 tool_call 解析（name + arguments）
 *  - 多 tool_call（多工具并行）
 *  - 无 tool_call → 空列表（视为最终回答）
 *  - 畸形 JSON → 全无效 → null（回灌重试）
 *  - 缺失 name → 无效
 *  - arguments 缺失/字符串形式
 *  - containsToolCall 判断
 */
public class TestToolCallParser {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        testValidSingle();
        testValidMultiple();
        testNoToolCall();
        testMalformedJson();
        testMissingName();
        testMissingArguments();
        testArgumentsAsString();
        testContainsToolCall();
        testAnthropicXmlFormat();
        testAnthropicXmlWithParametersJson();
        testAnthropicXmlInvalid();
        testMalformedClosingTag();
        testPluralToolCalls();
        testMixedSingularPlural();
        testPluralMalformed();
        System.out.println("\n======== TestToolCallParser: " + pass + " passed, " + fail + " failed ========");
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

    private static void testValidSingle() {
        String out = "我来查一下仓库\n<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("valid single: 1 call", calls != null && calls.size() == 1);
        if (calls != null && !calls.isEmpty()) {
            check("valid single: name=list_repos", "list_repos".equals(calls.get(0).name));
            check("valid single: arguments empty obj", calls.get(0).arguments != null && calls.get(0).arguments.isEmpty());
        }
    }

    private static void testValidMultiple() {
        String out = "<tool_call>{\"name\":\"list_repos\",\"arguments\":{}}</tool_call>"
                + "\n<tool_call>{\"name\":\"get_repos\",\"arguments\":{\"vid\":1}}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("valid multiple: 2 calls", calls != null && calls.size() == 2);
        if (calls != null && calls.size() == 2) {
            check("multiple: first name", "list_repos".equals(calls.get(0).name));
            check("multiple: second name", "get_repos".equals(calls.get(1).name));
            check("multiple: second vid=1", calls.get(1).arguments.getInteger("vid") == 1);
        }
    }

    private static void testNoToolCall() {
        String out = "直接回答用户的问题，不需要工具。";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("no tool_call -> empty list (final answer)", calls != null && calls.isEmpty());
        check("null/empty -> empty list", ToolCallParser.parse(null).isEmpty());
        check("blank -> empty list", ToolCallParser.parse("   ").isEmpty());
    }

    private static void testMalformedJson() {
        String out = "<tool_call>{broken json}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("malformed json -> null (retry)", calls == null);
    }

    private static void testMissingName() {
        String out = "<tool_call>{\"arguments\":{}}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("missing name -> null (retry)", calls == null);
    }

    private static void testMissingArguments() {
        String out = "<tool_call>{\"name\":\"list_repos\"}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("missing arguments -> ok, empty args", calls != null && calls.size() == 1
                && calls.get(0).arguments != null && calls.get(0).arguments.isEmpty());
    }

    private static void testArgumentsAsString() {
        String out = "<tool_call>{\"name\":\"get_repos\",\"arguments\":\"{\\\"vid\\\":3}\"}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("arguments as string -> parsed", calls != null && calls.size() == 1
                && calls.get(0).arguments.getInteger("vid") == 3);
    }

    private static void testContainsToolCall() {
        check("containsToolCall true", ToolCallParser.containsToolCall("<tool_call>{}</tool_call>"));
        check("containsToolCall false", !ToolCallParser.containsToolCall("no marker here"));
    }

    /** T5.3 容错：模型偶发输出 Anthropic/Claude XML 格式 */
    private static void testAnthropicXmlFormat() {
        String out = "<tool_call>\n<invoke name=\"list_docs\">\n<parameter name=\"vid\">8</parameter>\n</invoke>\n</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("anthropic xml: parsed", calls != null && calls.size() == 1);
        if (calls != null && !calls.isEmpty()) {
            check("anthropic xml: name=list_docs", "list_docs".equals(calls.get(0).name));
            check("anthropic xml: vid=8 (number)", calls.get(0).arguments.getInteger("vid") == 8);
        }
    }

    private static void testAnthropicXmlWithParametersJson() {
        String out = "<tool_call><invoke name=\"get_repos\"><parameters>{\"vid\":3}</parameters></invoke></tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("anthropic xml+parameters json: parsed", calls != null && calls.size() == 1
                && calls.get(0).arguments.getInteger("vid") == 3);
    }

    private static void testAnthropicXmlInvalid() {
        String out = "<tool_call><invoke name=\"\"></invoke></tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("anthropic xml empty name -> null", calls == null);
    }

    /** 关闭标签写错（如 </ce_tool_call>）→ 按畸形处理（null 重试），而非当最终回答 */
    private static void testMalformedClosingTag() {
        String out = "先查看一下。<tool_call>{\"name\":\"list_docs\",\"arguments\":{\"vid\":8}}</ce_tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("malformed closing tag -> null (retry)", calls == null);
        // 输出含 <tool_call 标记但无法解析 → 按畸形处理（重试），避免把残缺调用当最终回答
        check("stray <tool_call marker -> null (retry)",
                ToolCallParser.parse("请使用 <tool_call 格式调用工具") == null);
    }

    /** T8.1 容错：模型漂移输出复数包装 <tool_calls>...</tool_calls>（Anthropic 风格） */
    private static void testPluralToolCalls() {
        String out = "<tool_calls>\n{\"name\":\"list_repos\",\"arguments\":{}}\n</tool_calls>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("plural <tool_calls>: parsed 1 call", calls != null && calls.size() == 1);
        if (calls != null && !calls.isEmpty()) {
            check("plural: name=list_repos", "list_repos".equals(calls.get(0).name));
            check("plural: args empty", calls.get(0).arguments != null && calls.get(0).arguments.isEmpty());
        }
        check("containsToolCall(plural) true", ToolCallParser.containsToolCall(out));
        // 复数带参
        String out2 = "<tool_calls>\n{\"name\":\"get_doc\",\"arguments\":{\"vid\":8,\"docId\":123}}\n</tool_calls>";
        List<ToolCall> calls2 = ToolCallParser.parse(out2);
        check("plural with args: parsed", calls2 != null && calls2.size() == 1
                && calls2.get(0).arguments.getInteger("vid") == 8
                && calls2.get(0).arguments.getLong("docId") == 123L);
    }

    /** 混合：复数包装 + 单数并列 */
    private static void testMixedSingularPlural() {
        String out = "<tool_calls>{\"name\":\"list_repos\",\"arguments\":{}}</tool_calls>"
                + "\n<tool_call>{\"name\":\"get_repos\",\"arguments\":{\"vid\":1}}</tool_call>";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("mixed singular+plural: 2 calls", calls != null && calls.size() == 2);
        if (calls != null && calls.size() == 2) {
            check("mixed: first list_repos", "list_repos".equals(calls.get(0).name));
            check("mixed: second get_repos", "get_repos".equals(calls.get(1).name));
        }
    }

    /** 复数未闭合 → 畸形（null 重试），而非当最终回答 */
    private static void testPluralMalformed() {
        String out = "<tool_calls>\n{\"name\":\"list_repos\",\"arguments\":{}}";
        List<ToolCall> calls = ToolCallParser.parse(out);
        check("plural unclosed -> null (retry)", calls == null);
    }
}
