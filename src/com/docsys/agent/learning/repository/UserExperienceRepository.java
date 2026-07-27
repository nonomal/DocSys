package com.docsys.agent.learning.repository;

import com.docsys.agent.learning.entity.UserExperience;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * User Experience Repository - 用户经验仓储
 * MyBatis mapper interface.
 */
public interface UserExperienceRepository {

    List<UserExperience> findByUserId(@Param("userId") String userId);

    List<UserExperience> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);

    List<UserExperience> findByTaskType(@Param("taskType") String taskType);

    List<UserExperience> findSuccessByUserId(@Param("userId") String userId, @Param("limit") int limit);

    List<UserExperience> findSuccessByUserIdAndTaskType(@Param("userId") String userId, @Param("taskType") String taskType, @Param("limit") int limit);

    List<Map<String, Object>> countByTaskType(@Param("userId") String userId);

    Double getAverageRating(@Param("userId") String userId);

    List<UserExperience> findRecentSuccess(@Param("since") LocalDateTime since, @Param("limit") int limit);

    int deleteByUserId(@Param("userId") String userId);

    int insert(UserExperience experience);

    int updateById(UserExperience experience);

    int deleteById(@Param("id") Long id);
}
