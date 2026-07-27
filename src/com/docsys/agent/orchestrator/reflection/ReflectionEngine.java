package com.docsys.agent.orchestrator.reflection;

import com.docsys.agent.evolution.ExperienceMemory;
import com.docsys.agent.evolution.SelfDiagnostics;
import com.docsys.agent.llm.LLMService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * LLM-driven reflection step. Aggregates ErrorContext + experience history +
 * diagnostics, calls LLM, parses the JSON response into a ReflectionVerdict.
 *
 * Downgraded to fastjson 1.x + Java 1.8 syntax (text block replaced with
 * String concatenation) for the gnhf/docsysagent-314649 base.
 */
@Service
public class ReflectionEngine {

    private static final Logger log = LoggerFactory.getLogger(ReflectionEngine.class);

    private final LLMService llm;

    public ReflectionEngine(LLMService llm) {
        this.llm = llm;
    }

    public ReflectionVerdict evaluate(String userQuery, String intent, ErrorContext error) {
        String evidence = buildEvidence(userQuery, intent, error);
        String prompt = ""
            + "You are a reflection engine. Given the failure evidence below, decide\n"
            + "whether to replan. Reply with strict JSON:\n"
            + "{\"shouldReplan\":bool,\"replanHint\":string,\"confidence\":0..1,\"reason\":string}\n\n"
            + "Evidence:\n" + evidence + "\n";
        try {
            String raw = llm.chat(prompt, "reflection-" + UUID.randomUUID());
            return parseVerdict(raw);
        } catch (Exception e) {
            log.warn("ReflectionEngine LLM call failed, falling back to giveUp: {}",
                e.getMessage());
            return ReflectionVerdict.giveUp("llm-error: " + e.getMessage());
        }
    }

    ReflectionVerdict parseVerdict(String raw) {
        if (raw == null) return ReflectionVerdict.giveUp("null-llm-response");
        try {
            JSONObject n = JSONObject.parseObject(raw);
            if (n == null) {
                return ReflectionVerdict.giveUp("null-llm-response");
            }
            boolean replan = n.containsKey("shouldReplan") && n.getBoolean("shouldReplan");
            String hint    = n.containsKey("replanHint") ? n.getString("replanHint") : "";
            double conf    = n.containsKey("confidence") ? n.getDouble("confidence") : 0.0;
            String reason  = n.containsKey("reason") ? n.getString("reason") : "";
            return new ReflectionVerdict(replan, hint, conf, reason);
        } catch (Exception e) {
            log.warn("ReflectionEngine JSON parse failed: {}", e.getMessage());
            return ReflectionVerdict.giveUp("json-parse-error: " + e.getMessage());
        }
    }

    String buildEvidence(String userQuery, String intent, ErrorContext error) {
        StringBuilder sb = new StringBuilder();
        sb.append("intent=").append(intent).append('\n');
        if (error != null) {
            sb.append("errorCode=").append(error.errorCode()).append('\n');
            sb.append("errorMessage=").append(error.message()).append('\n');
        }
        try {
            sb.append("recentHistory=").append(
                ExperienceMemory.getInstance().recentForIntent(intent, 5)).append('\n');
            sb.append("recentDiagnostics=").append(
                SelfDiagnostics.getInstance().recentByOperation("execute_subtask", 5))
                .append('\n');
        } catch (Exception ignored) { /* evidence is best-effort */ }
        sb.append("userQuery=").append(userQuery).append('\n');
        return sb.toString();
    }
}
