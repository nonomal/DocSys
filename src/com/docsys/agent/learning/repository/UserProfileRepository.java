package com.docsys.agent.learning.repository;

import com.docsys.agent.learning.entity.UserPermission;
import java.util.List;

/**
 * User Profile Repository - 用户画像仓储
 * 基于UserPermission表构建用户画像
 * MyBatis mapper interface.
 */
public interface UserProfileRepository extends UserPermissionRepository {

    List<UserPermission> findAll();
}
