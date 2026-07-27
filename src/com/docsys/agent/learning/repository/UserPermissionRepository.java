package com.docsys.agent.learning.repository;

import com.docsys.agent.learning.entity.UserPermission;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * User Permission Repository - 用户权限仓储
 * MyBatis mapper interface.
 */
public interface UserPermissionRepository {

    List<UserPermission> findByUserId(@Param("userId") String userId);

    List<UserPermission> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);

    List<String> findAllUserIds();

    List<String> findUserIdsByTenantId(@Param("tenantId") String tenantId);

    int deleteByUserId(@Param("userId") String userId);

    int insert(UserPermission permission);

    int updateById(UserPermission permission);

    int deleteById(@Param("id") Long id);
}
