package com.DocSystem.agent.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Prometheus metrics for DocSysAgent operations.
 * Exposes timers for agent execution and LLM calls, and a gauge for active sessions.
 */
@Component
public class AgentMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    @Autowired
    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register active sessions gauge
        Gauge.builder("agent.active.sessions", activeSessions, AtomicInteger::get)
            .description("Number of currently active agent sessions")
            .register(meterRegistry);
    }

    /**
     * Record agent task execution duration and outcome.
     */
    public void recordExecution(String intent, long durationMs, boolean success) {
        Timer.builder("agent.execution")
            .description("Agent task execution time")
            .tag("intent", intent != null ? intent : "unknown")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    /**
     * Record LLM API call duration and outcome.
     */
    public void recordLlmCall(String model, long durationMs, boolean success) {
        Timer.builder("llm.call")
            .description("LLM API call duration")
            .tag("model", model != null ? model : "unknown")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    /**
     * Increment active session count.
     */
    public void sessionOpened() {
        activeSessions.incrementAndGet();
    }

    /**
     * Decrement active session count.
     */
    public void sessionClosed() {
        activeSessions.decrementAndGet();
    }

    /**
     * Set current active session count (used on startup/recovery).
     */
    public void recordActiveSessions(int count) {
        activeSessions.set(Math.max(0, count));
    }

    /**
     * Increment a generic counter.
     */
    public void incrementCounter(String name, String... tags) {
        Counter.builder(name)
            .description(name)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
}
