package com.DocSystem.agent.repository;

import com.DocSystem.agent.audit.StepAuditEntity;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for agent_step_audits table（T8.5）。
 * 包位于 com.DocSystem.agent.repository —— 已被 MapperScannerConfigurer 扫描。
 */
public interface StepAuditRepository {

    int insert(StepAuditEntity entity);

    /** 最近 N 条（按 id 倒序），供查询验证 */
    java.util.List<StepAuditEntity> selectRecent(@Param("limit") int limit);

    /** 某请求的全部步骤（按 id 升序） */
    java.util.List<StepAuditEntity> selectByRequestId(@Param("requestId") String requestId);

    /** 某会话的全部步骤（按 id 升序） */
    java.util.List<StepAuditEntity> selectBySessionId(@Param("sessionId") String sessionId);
}
