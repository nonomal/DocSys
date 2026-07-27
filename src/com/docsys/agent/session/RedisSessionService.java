package com.docsys.agent.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redis session service stub - disabled for Spring 4.x.
 * Spring Data Redis requires Spring Boot and is not available in native Spring 4.
 * Sessions are stored in MySQL via SessionService instead.
 *
 * This service remains for API compatibility but does nothing.
 * Set @Profile("!dev") to disable this class.
 */
public class RedisSessionService {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionService.class);
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String CLIENT_KEY_PREFIX = "session:client:";
    private static final String API_KEY_PREFIX = "agent:api-key:";

    public RedisSessionService() {
        log.info("RedisSessionService disabled - Spring Data Redis not available in Spring 4.x. Use SessionService for MySQL-backed session storage.");
    }

    public static class SessionData {
        public String username;
        public String sessionId;
        public String jsessionid;
        public String tenantId;
        public String metadataJson;

        public SessionData() {}

        public SessionData(String username, String sessionId, String jsessionid) {
            this.username = username;
            this.sessionId = sessionId;
            this.jsessionid = jsessionid;
        }

        public SessionData(String username, String sessionId, String jsessionid, String tenantId) {
            this.username = username;
            this.sessionId = sessionId;
            this.jsessionid = jsessionid;
            this.tenantId = tenantId;
        }
    }

    public void saveSession(String sessionId, String username, String jsessionid) {
        log.debug("Redis disabled - skipping saveSession");
    }

    public void saveSession(String sessionId, String username, String jsessionid, String tenantId) {
        log.debug("Redis disabled - skipping saveSession");
    }

    public Optional<SessionData> findSession(String sessionId) {
        return Optional.empty();
    }

    public void deleteSession(String sessionId) {
        log.debug("Redis disabled - skipping deleteSession");
    }

    public void touchSession(String sessionId) {
        log.debug("Redis disabled - skipping touchSession");
    }

    public long countActiveSessions() {
        return 0L;
    }

    public void saveClientCookie(String sessionId, String cookieValue) {
        log.debug("Redis disabled - skipping saveClientCookie");
    }

    public Optional<String> getClientCookie(String sessionId) {
        return Optional.empty();
    }

    public void saveApiKey(String apiKey, String owner) {
        log.debug("Redis disabled - skipping saveApiKey");
    }

    public Optional<String> validateApiKey(String apiKey) {
        return Optional.empty();
    }
}