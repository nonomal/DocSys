package com.DocSystem.agent.monitoring;

import com.DocSystem.agent.client.DocSysClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for DocSystem backend connectivity.
 * Probes the /User/isLogin.do endpoint to verify the backend is reachable.
 *
 * This replaces Spring Boot Actuator's HealthIndicator.
 */
@Component
public class DocSysHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DocSysHealthIndicator.class);

    @Autowired
    private DocSysClient docSysClient;

    public java.util.Map<String, Object> health() {
        try {
            Map<String, Object> userInfo = docSysClient.getLoginUser();
            if (userInfo == null) {
                return Health.unknown()
                    .withDetail("error", "Null response from DocSystem")
                    .withDetail("docSystemUrl", docSysClient.getBaseUrl())
                    .build();
            }
            String status = (String) userInfo.get("status");
            if ("ok".equals(status)) {
                Object data = userInfo.get("data");
                String user = data instanceof java.util.Map
                    ? (String) ((java.util.Map<?, ?>) data).get("name")
                    : String.valueOf(data);
                return Health.up()
                    .withDetail("docSystemUrl", docSysClient.getBaseUrl())
                    .withDetail("currentUser", user)
                    .build();
            }
            return Health.down()
                .withDetail("docSystemUrl", docSysClient.getBaseUrl())
                .withDetail("status", status)
                .withDetail("msgInfo", userInfo.get("msgInfo"))
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("docSystemUrl", docSysClient.getBaseUrl())
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
