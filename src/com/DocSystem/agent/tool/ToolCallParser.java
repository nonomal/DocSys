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

    /** 匹配单个 tool_call 块（非贪婪，支持跨行） */
    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("<tool_call>(.*?)</tool_call>", Pattern.DOTALL);

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
            String body = m.group(1).trim();
            ToolCall call = parseOne(body, output);
            if (call == null) {
                anyInvalid = true;
            } else {
                calls.add(call);
            }
        }

        if (!sawMarker) {
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

    private static ToolCall parseOne(String body, String rawOutput) {
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
            log.warn("ToolCallParser: invalid JSON in tool_call: {}", body);
            return null;
        }
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
