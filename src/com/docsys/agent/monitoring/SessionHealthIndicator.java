package com.docsys.agent.monitoring;

import com.docsys.agent.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Health indicator for agent sessions from MySQL persistence layer.
 * Reports persisted session count from the database.
 *
 * This replaces Spring Boot Actuator's HealthIndicator.
 */
@Component
public class SessionHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(SessionHealthIndicator.class);

    @Autowired
    private SessionService sessionService;

    public java.util.Map<String, Object> health() {
        try {
            long persistedCount = sessionService.countActiveSessions();
            return Health.up()
                .withDetail("persistedSessions", persistedCount)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Could not query session count: " + e.getMessage())
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
