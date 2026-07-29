package com.DocSystem.agent.orchestrator.reflection;

/**
 * Error context for reflection: Java 1.8-compatible class (replaces a
 * Java 16 record, which the gnhf/docsysagent-314649 base does not support).
 */
public final class ErrorContext {
    private final String errorCode;
    private final String message;
    private final String attemptedAction;
    private final String partialOutput;

    public ErrorContext(String errorCode, String message,
                        String attemptedAction, String partialOutput) {
        this.errorCode = errorCode;
        this.message = message;
        this.attemptedAction = attemptedAction;
        this.partialOutput = partialOutput;
    }

    public String errorCode() { return errorCode; }
    public String message() { return message; }
    public String attemptedAction() { return attemptedAction; }
    public String partialOutput() { return partialOutput; }
}
