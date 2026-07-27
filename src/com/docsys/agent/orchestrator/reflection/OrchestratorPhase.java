package com.docsys.agent.orchestrator.reflection;

public enum OrchestratorPhase {
    DECOMPOSE, EXECUTE, REFLECT, REPLAN, DONE;

    public static OrchestratorPhase fromString(String s) {
        if (s == null) return DONE;
        try { return OrchestratorPhase.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return DONE; }
    }
}
