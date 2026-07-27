package com.docsys.agent.orchestrator.reflection;

import com.docsys.agent.core.AgentResponse;
import com.docsys.agent.orchestrator.MainAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure orchestration driver. Holds no Spring state. The executor function
 * abstracts the actual sub-task dispatch so this class can be unit-tested
 * without spinning up MainAgent.
 *
 * OFF path: call executor once with the initial plan, write an empty
 * ReflectionReport. This is the byte-for-byte equivalent of the pre-loop
 * MainAgent code path — keeping the 696 E2E tests green.
 */
public class OrchestrationLoop {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationLoop.class);

    private final ReflectionConfig config;
    private final ReflectionEngine engine;
    private final Replanner replanner;

    public OrchestrationLoop(ReflectionConfig config,
                             ReflectionEngine engine,
                             Replanner replanner) {
        this.config = config;
        this.engine = engine;
        this.replanner = replanner;
    }

    public Outcome run(String userQuery, String intent,
                       MainAgent.TaskDecomposition initialPlan,
                       Function<MainAgent.TaskDecomposition, Optional<StepResult>> executor) {

        if (!config.isEnabled()) {
            // OFF path — call once, no reflection.
            StepResult r = executor.apply(initialPlan)
                .orElseGet(() -> new StepResult(false,
                    new ErrorContext("EXECUTOR_EMPTY", "no-result", null, null), null));
            AgentResponse resp = toResponse(r, ReflectionReport.empty());
            return new Outcome(resp, ReflectionReport.empty());
        }

        int attempts = 0;
        List<ReflectionVerdict> verdicts = new ArrayList<>();
        MainAgent.TaskDecomposition plan = initialPlan;
        StepResult last = null;
        String lastError = null;

        // Hard cap: at most `maxAttempts` reflection evaluations. Initial
        // execute (attempt 0) is not counted; first REFLECT fires on first
        // failure and consumes attempt #1.
        while (attempts < config.getMaxAttempts()) {
            StepResult r = executor.apply(plan)
                .orElseGet(() -> new StepResult(false,
                    new ErrorContext("EXECUTOR_EMPTY", "no-result", null, null), null));
            last = r;
            if (r.success) {
                ReflectionReport report = new ReflectionReport(
                    attempts, verdicts, plan, lastError);
                return new Outcome(toResponse(r, report), report);
            }
            lastError = r.error != null ? r.error.message() : "unknown";
            ReflectionVerdict v = engine.evaluate(userQuery, intent, r.error);
            verdicts.add(v);
            attempts++;
            if (!v.shouldReplan()) break;
            if (attempts >= config.getMaxAttempts()) break;
            try {
                plan = replanner.replan(userQuery, intent, plan, v);
            } catch (Exception ex) {
                log.warn("OrchestrationLoop replanner threw, giving up: {}", ex.getMessage());
                break;
            }
        }
        AgentResponse errResp = AgentResponse.error(
            "Reflection loop exhausted: " + lastError, "REFLECTION_EXHAUSTED");
        if (last != null) {
            errResp.withData(last.partialOutput);
        }
        ReflectionReport report = new ReflectionReport(attempts, verdicts, plan, lastError);
        return new Outcome(errResp, report);
    }

    private AgentResponse toResponse(StepResult r, ReflectionReport report) {
        AgentResponse resp = r.success
            ? AgentResponse.ok(r.partialOutput)
            : AgentResponse.error(r.error != null ? r.error.message() : "unknown");
        resp.addMetadata("reflection", report);
        return resp;
    }

    /** Executor contract: success OR (error + optional partial output). */
    public static final class StepResult {
        private final boolean success;
        private final ErrorContext error;
        private final Object partialOutput;

        public StepResult(boolean success, ErrorContext error, Object partialOutput) {
            this.success = success;
            this.error = error;
            this.partialOutput = partialOutput;
        }

        public boolean success() { return success; }
        public ErrorContext error() { return error; }
        public Object partialOutput() { return partialOutput; }
    }

    public static final class Outcome {
        private final AgentResponse response;
        private final ReflectionReport report;

        public Outcome(AgentResponse response, ReflectionReport report) {
            this.response = response;
            this.report = report;
        }

        public AgentResponse response() { return response; }
        public ReflectionReport report() { return report; }
    }
}
