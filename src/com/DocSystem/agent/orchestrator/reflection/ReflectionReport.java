package com.DocSystem.agent.orchestrator.reflection;

import java.util.Collections;
import java.util.List;

/**
 * Reflection report: Java 1.8-compatible class (replaces a Java 16 record).
 */
public final class ReflectionReport {
    private final int attempts;
    private final List<ReflectionVerdict> verdicts;
    private final Object finalPlan;
    private final String originalError;

    public ReflectionReport(int attempts, List<ReflectionVerdict> verdicts,
                            Object finalPlan, String originalError) {
        this.attempts = attempts;
        this.verdicts = verdicts;
        this.finalPlan = finalPlan;
        this.originalError = originalError;
    }

    public static ReflectionReport empty() {
        return new ReflectionReport(0, Collections.emptyList(), null, null);
    }

    public int attempts() { return attempts; }
    public List<ReflectionVerdict> verdicts() { return verdicts; }
    public Object finalPlan() { return finalPlan; }

    /** Redact obvious secret-shaped substrings (>=8 contiguous alnum/- chars after a keyword). */
    static String redact(String in) {
        if (in == null) return null;
        return in.replaceAll(
            "(?i)(token|ssn|password|secret|key)[=:]\\s*[A-Za-z0-9\\-]{8,}",
            "$1=<redacted>");
    }

    public String originalError() { return redact(originalError); }

    @Override
    public String toString() {
        return "ReflectionReport{attempts=" + attempts
            + ", verdicts=" + verdicts.size()
            + ", hasFinalPlan=" + (finalPlan != null)
            + ", originalError=" + redact(originalError) + "}";
    }
}
