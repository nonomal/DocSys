package com.DocSystem.agent.monitoring;

import com.DocSystem.agent.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Health indicator for LLM service availability.
 * Uses the existing LLMService.isAvailable() probe.
 *
 * This replaces Spring Boot Actuator's HealthIndicator.
 */
@Component
public class LLMHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(LLMHealthIndicator.class);

    @Autowired(required = false)
    private LLMService llmService;

    public java.util.Map<String, Object> health() {
        if (llmService == null) {
            return Health.unknown()
                .withDetail("error", "LLMService not configured (llm.api-key may be missing)")
                .build();
        }
        try {
            boolean available = llmService.isAvailable();
            if (available) {
                return Health.up()
                    .withDetail("available", true)
                    .build();
            } else {
                return Health.unknown()
                    .withDetail("available", false)
                    .withDetail("error", "LLM endpoint not reachable or returned 5xx")
                    .build();
            }
        } catch (Exception e) {
            return Health.unknown()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * Simple Health class replacing Spring Boot Actuator Health.
     */
    public static class Health {
        private final Status status;
        private final java.util.Map<String, Object> details = new java.util.HashMap<>();

        public static Health up() {
            return new Health(Status.UP);
        }

        public static Health down() {
            return new Health(Status.DOWN);
        }

        public static Health unknown() {
            return new Health(Status.UNKNOWN);
        }

        private Health(Status status) {
            this.status = status;
        }

        public Health withDetail(String key, Object value) {
            details.put(key, value);
            return this;
        }

        public java.util.Map<String, Object> build() {
            return toMap();
        }

        public Status getStatus() {
            return status;
        }

        public java.util.Map<String, Object> getDetails() {
            return details;
        }

        public java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", status.name());
            result.putAll(details);
            return result;
        }
    }

    public enum Status {
        UP, DOWN, UNKNOWN
    }
}
