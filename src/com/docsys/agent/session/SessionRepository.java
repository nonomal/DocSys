package com.docsys.agent.session;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for agent_sessions table.
 * Replaces JPA JpaRepository with plain MyBatis mapper interface.
 */
public interface SessionRepository {

    SessionEntity selectBySessionId(String sessionId);

    List<SessionEntity> selectByUsername(String username);

    int insert(SessionEntity session);

    int updateLastActive(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    int deleteBySessionId(String sessionId);

    int deleteInactiveBefore(LocalDateTime cutoff);

    long count();
}