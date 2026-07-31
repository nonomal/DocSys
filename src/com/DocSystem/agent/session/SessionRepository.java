package com.DocSystem.agent.session;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for agent_sessions table.
 * Replaces JPA JpaRepository with plain MyBatis mapper interface.
 */
public interface SessionRepository {

    SessionEntity selectBySessionId(@Param("sessionId") String sessionId);

    List<SessionEntity> selectByUsername(@Param("username") String username);

    int insert(SessionEntity session);

    int updateLastActive(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    int updateMetadata(@Param("sessionId") String sessionId, @Param("metadata") String metadata);

    int deleteBySessionId(@Param("sessionId") String sessionId);

    int deleteInactiveBefore(@Param("cutoff") LocalDateTime cutoff);

    long count();
}