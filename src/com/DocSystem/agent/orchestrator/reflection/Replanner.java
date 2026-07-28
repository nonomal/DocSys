package com.DocSystem.agent.orchestrator.reflection;

import com.DocSystem.agent.llm.LLMService;
import com.DocSystem.agent.orchestrator.MainAgent;
import com.DocSystem.agent.orchestrator.SubTask;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LLM-driven replanner. Given the previous TaskDecomposition and a
 * ReflectionVerdict, asks the LLM for a new plan and rewrites every subTask id
 * (UUID) to guarantee no id collision with the previous plan.
 *
 * Note: This class references MainAgent.TaskDecomposition which is widened
 * from `private static` to `public static` in T9 of the implementation plan.
 */
@Service
public class Replanner {

    private static final Logger log = LoggerFactory.getLogger(Replanner.class);
    private static final int MAX_SUBTASKS = 16;

    private final LLMService llm;

    public Replanner(LLMService llm) {
        this.llm = llm;
    }

    public MainAgent.TaskDecomposition replan(
            String userQuery, String intent,
            MainAgent.TaskDecomposition previous,
            ReflectionVerdict verdict) {

        String prevJson;
        try { prevJson = JSON.toJSONString(previous); }
        catch (Exception e) { prevJson = previous.toString(); }

        String prompt = ""
            + "You are a replanner. Given the previous plan and a reflection hint,\n"
            + "return a strict JSON plan:\n"
            + "{\"subTasks\":[{\"id\":string,\"description\":string,\"params\":{}}]}\n\n"
            + "Previous plan:\n" + prevJson + "\n\n"
            + "Reflection verdict: shouldReplan=" + verdict.shouldReplan()
            + " hint=" + verdict.replanHint()
            + " reason=" + verdict.reason() + "\n"
            + "userQuery=" + userQuery + " intent=" + intent + "\n";

        try {
            String raw = llm.chat(prompt, "replan-" + UUID.randomUUID());
            return parseAndRewire(raw);
        } catch (Exception e) {
            log.warn("Replanner LLM call failed, returning previous plan unchanged: {}",
                e.getMessage());
            return previous; // give-up by returning prev; caller treats !success as final failure
        }
    }

    MainAgent.TaskDecomposition parseAndRewire(String raw) throws Exception {
        if (raw == null) throw new IllegalStateException("null-llm-response");
        JSONObject root = JSON.parseObject(raw);
        if (root == null) throw new IllegalStateException("null-json");
        JSONArray arr = root.getJSONArray("subTasks");
        if (arr == null || arr.isEmpty()) {
            throw new IllegalStateException("empty-plan");
        }
        MainAgent.TaskDecomposition out = new MainAgent.TaskDecomposition();
        int n = Math.min(arr.size(), MAX_SUBTASKS);
        if (arr.size() > MAX_SUBTASKS) {
            log.warn("Replanner truncated plan from {} to {} subtasks",
                arr.size(), MAX_SUBTASKS);
        }
        for (int i = 0; i < n; i++) {
            JSONObject st = arr.getJSONObject(i);
            if (st == null) continue;
            // CRITICAL: always rewrite id to a new UUID — never reuse LLM-suggested id
            String newId = "rt-" + UUID.randomUUID();
            String desc = st.getString("description");
            if (desc == null) desc = "(no description)";
            Map<String, String> params = new HashMap<>();
            JSONObject p = st.getJSONObject("params");
            if (p != null) {
                for (java.util.Map.Entry<String, Object> e : p.entrySet()) {
                    if (e.getValue() != null) {
                        params.put(e.getKey(), e.getValue().toString());
                    }
                }
            }
            out.addSubTask(new SubTask(newId, desc, params));
        }
        return out;
    }
}
