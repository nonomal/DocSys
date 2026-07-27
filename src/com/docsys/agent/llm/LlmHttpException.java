package com.docsys.agent.llm;

import java.io.IOException;

/**
 * Signals an HTTP error response from the LLM API (4xx or 5xx).
 * Extends IOException so Resilience4j's circuit breaker (configured with
 * java.io.IOException in recordExceptions) will record it and trigger
 * the fallback without requiring module-system flags on Java 17+.
 */
public class LlmHttpException extends IOException {

    private final int statusCode;

    public LlmHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
