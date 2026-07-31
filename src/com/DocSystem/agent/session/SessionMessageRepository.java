package com.DocSystem.agent.session;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for agent_session_messages table.
 */
public interface SessionMessageRepository {

    int insert(SessionMessageEntity message);

    /** 会话内最大 seq（无消息 → 0） */
    Integer maxSeq(@Param("sessionId") String sessionId);

    /** 按 seq 升序取会话全部消息 */
    List<SessionMessageEntity> selectBySessionIdOrderBySeq(@Param("sessionId") String sessionId);

    /** 会话消息条数 */
    int countBySessionId(@Param("sessionId") String sessionId);

    int deleteBySessionId(@Param("sessionId") String sessionId);
}
