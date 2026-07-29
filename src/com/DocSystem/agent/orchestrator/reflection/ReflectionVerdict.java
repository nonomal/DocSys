package com.DocSystem.agent.orchestrator.reflection;

/**
 * Reflection verdict: Java 1.8-compatible class (replaces a Java 16 record).
 */
public final class ReflectionVerdict {
    private final boolean shouldReplan;
    private final String replanHint;
    private final double confidence;
    private final String reason;

    public ReflectionVerdict(boolean shouldReplan, String replanHint,
                             double confidence, String reason) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                "confidence must be in [0,1], got " + confidence);
        }
        this.shouldReplan = shouldReplan;
        this.replanHint = replanHint;
        this.confidence = confidence;
        this.reason = reason;
    }

    public boolean shouldReplan() { return shouldReplan; }
    public String replanHint() { return replanHint; }
    public double confidence() { return confidence; }
    public String reason() { return reason; }

    public static ReflectionVerdict giveUp(String reason) {
        return new ReflectionVerdict(false, "", 0.0, reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReflectionVerdict)) return false;
        ReflectionVerdict v = (ReflectionVerdict) o;
        return shouldReplan == v.shouldReplan
            && Double.compare(confidence, v.confidence) == 0
            && (replanHint == null ? v.replanHint == null : replanHint.equals(v.replanHint))
            && (reason == null ? v.reason == null : reason.equals(v.reason));
    }

    @Override
    public int hashCode() {
        int h = (shouldReplan ? 1 : 0);
        h = 31 * h + (replanHint == null ? 0 : replanHint.hashCode());
        h = 31 * h + Double.hashCode(confidence);
        h = 31 * h + (reason == null ? 0 : reason.hashCode());
        return h;
    }
}
