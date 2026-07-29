package com.DocSystem.agent.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Session persistence service backed by MySQL.
 * Replaces in-memory ConcurrentHashMap in AgentController.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private SessionRepository sessionRepository;

    /**
     * Create or update a session.
     */
    @Transactional
    public void save(SessionEntity session) {
        SessionEntity existing = sessionRepository.selectBySessionId(session.getSessionId());
        if (existing != null) {
            existing.setUsername(session.getUsername());
            existing.setJsessionid(session.getJsessionid());
            existing.setTenantId(session.getTenantId());
            existing.setLastActive(LocalDateTime.now());
            sessionRepository.updateLastActive(session.getSessionId(), LocalDateTime.now());
        } else {
            session.setCreatedAt(LocalDateTime.now());
            session.setLastActive(LocalDateTime.now());
            sessionRepository.insert(session);
        }
    }

    /**
     * Find a session by sessionId.
     */
    public SessionEntity findBySessionId(String sessionId) {
        return sessionRepository.selectBySessionId(sessionId);
    }

    /**
     * Touch last_active timestamp (called on each request to keep session alive).
     */
    @Transactional
    public void touch(String sessionId) {
        sessionRepository.updateLastActive(sessionId, LocalDateTime.now());
    }

    /**
     * Delete a session.
     */
    @Transactional
    public void delete(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }

    /**
     * Get total active session count.
     */
    public long countActiveSessions() {
        return sessionRepository.count();
    }

    /**
     * Periodic cleanup of inactive sessions (older than 7 days).
     * Runs every 6 hours.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    @Transactional
    public void cleanupInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = sessionRepository.deleteInactiveBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} inactive sessions older than {}", deleted, cutoff);
        }
    }
}
