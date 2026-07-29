package com.DocSystem.agent.learning.repository;

import com.DocSystem.agent.learning.entity.SharedKnowledge;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Shared Knowledge Repository - 共享知识仓储
 * MyBatis mapper interface.
 */
public interface SharedKnowledgeRepository {

    List<SharedKnowledge> findByTenantId(@Param("tenantId") String tenantId);

    List<SharedKnowledge> findVisibleToTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<SharedKnowledge> findByTaskTypeAndVisibility(@Param("taskType") String taskType, @Param("tenantId") String tenantId, @Param("limit") int limit);

    List<SharedKnowledge> findByAuthorId(@Param("authorId") String authorId);

    List<SharedKnowledge> findTopPublicKnowledge(@Param("limit") int limit);

    int incrementUsageCount(@Param("id") Long id);

    int insert(SharedKnowledge knowledge);

    int updateById(SharedKnowledge knowledge);

    int deleteById(@Param("id") Long id);
}
