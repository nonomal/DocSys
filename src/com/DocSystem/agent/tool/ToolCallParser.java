package com.DocSystem.agent.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具调用解析器 —— 解析 LLM 输出中的 {@code <tool_call>...</tool_call>} 块。
 *
 * <p>格式约定（与 ToolPromptBuilder 渲染的提示词一致）：</p>
 * <pre>
 * &lt;tool_call&gt;{"name":"list_repos","arguments":{}}&lt;/tool_call&gt;
 * </pre>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>LLM 输出多个 tool_call（多工具并行）→ 返回全部（按出现顺序）。</li>
 *   <li>JSON 解析失败 → 返回 null，由上层把原文回灌给 LLM 提示重试。</li>
 *   <li>缺失 name → 返回 null（视为无效调用，回灌重试）。</li>
 * </ul>
 *
 * <p>对应开发计划 T1.5。</p>
 */
public class ToolCallParser {

    private static final Logger log = LoggerFactory.getLogger(ToolCallParser.class);

    /**
     * 匹配 tool_call 块（非贪婪，支持跨行）。
     * 兼容两种形态：
     *  1) 单数：&lt;tool_call&gt;{"name":"...","arguments":{}}&lt;/tool_call&gt;
     *  2) 复数包装（Anthropic 风格）：&lt;tool_calls&gt;\n{"name":"..."}\n&lt;/tool_calls&gt;
     */
    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("<(tool_call|tool_calls)>(.*?)</(tool_call|tool_calls)>", Pattern.DOTALL);

    /**
     * 解析 LLM 输出中的全部工具调用。
     *
     * @param output LLM 原始输出
     * @return 工具调用列表（可能为空 → 表示无工具调用，输出即最终回答）；
     *         若输出含 tool_call 标记但全部解析失败 → 返回 null（调用方应回灌重试）
     */
    public static List<ToolCall> parse(String output) {
        if (output == null || output.isEmpty()) {
            return new ArrayList<>();
        }

        Matcher m = TOOL_CALL_PATTERN.matcher(output);
        List<ToolCall> calls = new ArrayList<>();
        boolean sawMarker = false;
        boolean anyInvalid = false;

        while (m.find()) {
            sawMarker = true;
            String body = m.group(2).trim();
            ToolCall call = parseOne(body, output);
            if (call == null) {
                anyInvalid = true;
            } else {
                calls.add(call);
            }
        }

        if (!sawMarker) {
            // 无任何完整的 tool_call 块。但若输出里出现 tool_call/invoke 相关标记
            // （如关闭标签写错 </ce_tool_call>、只有开标签等），视为畸形而非最终回答，
            // 让上层回灌重试（T5.3 模型输出容错）。
            if (looksLikeMalformedToolCall(output)) {
                log.warn("ToolCallParser: output contains tool_call marker but no parseable block, treating as malformed");
                return null;
            }
            // 无任何 tool_call 标记 → 视为最终回答
            return new ArrayList<>();
        }
        if (anyInvalid && calls.isEmpty()) {
            // 有标记但全部无效 → 通知调用方回灌重试
            return null;
        }
        // 部分有效：返回有效的（无效的忽略并告警）
        if (anyInvalid) {
            log.warn("ToolCallParser: some tool_call blocks were invalid, using {} valid calls", calls.size());
        }
        return calls;
    }

    /**
     * 判断输出中是否含 tool_call 标记。
     */
    public static boolean containsToolCall(String output) {
        return output != null && TOOL_CALL_PATTERN.matcher(output).find();
    }

    /** 输出里出现疑似工具调用标记但未解析出完整块（关闭标签写错/被截断/复数未闭合等） */
    private static boolean looksLikeMalformedToolCall(String output) {
        return output != null && (
                output.contains("<tool_call")
                || output.contains("<tool_calls")
                || output.contains("</tool_call>")
                || output.contains("</tool_calls>")
                || output.contains("<invoke ")
                || output.contains("</invoke>"));
    }

    private static ToolCall parseOne(String body, String rawOutput) {
        // 优先 JSON 格式：<tool_call>{"name":"...","arguments":{...}}</tool_call>
        try {
            JSONObject obj = JSON.parseObject(body);
            if (obj == null) {
                return null;
            }
            String name = obj.getString("name");
            if (name == null || name.trim().isEmpty()) {
                log.warn("ToolCallParser: tool_call missing 'name': {}", body);
                return null;
            }
            Object argsObj = obj.get("arguments");
            JSONObject args;
            if (argsObj == null) {
                args = new JSONObject();
            } else if (argsObj instanceof JSONObject) {
                args = (JSONObject) argsObj;
            } else {
                // arguments 是字符串或其它 → 尝试解析为 JSON
                try {
                    args = JSON.parseObject(argsObj.toString());
                } catch (JSONException e) {
                    log.warn("ToolCallParser: arguments not a JSON object: {}", body);
                    args = new JSONObject();
                }
            }
            return new ToolCall(name.trim(), args, rawOutput);
        } catch (JSONException e) {
            // T5.3：模型偶发输出 Anthropic/Claude XML 格式 <invoke name="x"><parameter name="k">v</parameter></invoke>
            ToolCall xmlCall = parseAnthropicXml(body, rawOutput);
            if (xmlCall != null) {
                log.warn("ToolCallParser: fell back to Anthropic XML format for: {}", body);
                return xmlCall;
            }
            log.warn("ToolCallParser: invalid JSON in tool_call: {}", body);
            return null;
        }
    }

    /**
     * T5.3 容错：解析 Anthropic/Claude 风格工具调用。
     * <pre>
     * &lt;invoke name="list_docs"&gt;
     *   &lt;parameter name="vid"&gt;8&lt;/parameter&gt;
     * &lt;/invoke&gt;
     * </pre>
     * 或 &lt;parameters&gt;{"vid":8}&lt;/parameters&gt; 形式。
     */
    private static ToolCall parseAnthropicXml(String body, String rawOutput) {
        try {
            Matcher invoke = Pattern.compile("<invoke\\s+name=[\"']([^\"']+)[\"'][^>]*>", Pattern.DOTALL).matcher(body);
            if (!invoke.find()) {
                return null;
            }
            String name = invoke.group(1).trim();
            if (name.isEmpty()) {
                return null;
            }
            JSONObject args = new JSONObject();
            // 形式 1：<parameter name="k">v</parameter>
            Matcher pm = Pattern.compile(
                    "<parameter\\s+name=[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</parameter>", Pattern.DOTALL)
                    .matcher(body);
            while (pm.find()) {
                String key = pm.group(1).trim();
                String val = pm.group(2).trim();
                if (key.isEmpty()) continue;
                putXmlParam(args, key, val);
            }
            // 形式 2：<parameters>{...}</parameters>
            if (args.isEmpty()) {
                Matcher pj = Pattern.compile("<parameters>([\\s\\S]*?)</parameters>", Pattern.DOTALL).matcher(body);
                if (pj.find()) {
                    try {
                        JSONObject parsed = JSON.parseObject(pj.group(1).trim());
                        if (parsed != null) {
                            args.putAll(parsed);
                        }
                    } catch (JSONException ignored) {}
                }
            }
            return new ToolCall(name, args, rawOutput);
        } catch (Exception e) {
            log.warn("ToolCallParser: failed to parse Anthropic XML tool_call: {}", body);
            return null;
        }
    }

    /** 把 XML 参数值按语义放入 JSON（数字/布尔自动转换，其余为字符串） */
    private static void putXmlParam(JSONObject args, String key, String val) {
        if ("null".equals(val)) {
            return;
        }
        // 数字
        try {
            if (val.matches("-?\\d+")) {
                args.put(key, Long.parseLong(val));
                return;
            }
            if (val.matches("-?\\d+\\.\\d+")) {
                args.put(key, Double.parseDouble(val));
                return;
            }
        } catch (NumberFormatException ignored) {}
        // 布尔
        if ("true".equals(val) || "false".equals(val)) {
            args.put(key, Boolean.parseBoolean(val));
            return;
        }
        args.put(key, val);
    }

    /**
     * 便捷：解析单个 tool_call 并取第一个（用于单工具场景）。
     *
     * @return 第一个有效调用；无调用或全无效 → null
     */
    public static ToolCall parseFirst(String output) {
        List<ToolCall> calls = parse(output);
        if (calls == null || calls.isEmpty()) {
            return null;
        }
        return calls.get(0);
    }
}
