package com.docsys.agent.learning.entity;

import java.util.Set;
import java.util.HashSet;

/**
 * User Profile - 用户画像
 * 用于协同过滤计算
 */
public class UserProfile {
    private String userId;
    private String tenantId;
    private String username;
    private Set<String> roles = new HashSet<>();
    private String departmentId;
    private Integer permissionLevel; // 0=普通, 1=管理员, 2=超级管理员

    public UserProfile() {}

    public UserProfile(String userId, String tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public Integer getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(Integer permissionLevel) { this.permissionLevel = permissionLevel; }

    public void addRole(String role) { this.roles.add(role); }
    public boolean hasPermissionLevel(int minLevel) {
        return this.permissionLevel != null && this.permissionLevel >= minLevel;
    }
}
