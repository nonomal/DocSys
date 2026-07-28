package com.DocSystem.agent.learning.repository;

import com.DocSystem.agent.learning.entity.UserBehaviorTag;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * User Behavior Tag Repository - 用户行为标签仓储
 * MyBatis mapper interface.
 */
public interface UserBehaviorTagRepository {

    List<UserBehaviorTag> findByUserId(@Param("userId") String userId);

    List<UserBehaviorTag> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);

    UserBehaviorTag findByUserIdAndTag(@Param("userId") String userId, @Param("tag") String tag);

    List<String> findAllUserIds();

    List<String> findTagsByUserId(@Param("userId") String userId);

    int deleteByUserId(@Param("userId") String userId);

    int insert(UserBehaviorTag tag);

    int updateById(UserBehaviorTag tag);

    int deleteById(@Param("id") Long id);
}
