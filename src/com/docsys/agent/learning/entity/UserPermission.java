package com.docsys.agent.learning.entity;

import java.time.LocalDateTime;

/**
 * User Permission Entity - 用户权限
 * POJO for MyBatis mapping.
 */
public class UserPermission {
    private Long id;
    private String userId;
    private String tenantId;
    private String roleName;
    private String departmentId;
    private Integer permissionLevel;
    private LocalDateTime createdAt;

    public UserPermission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public Integer getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(Integer permissionLevel) { this.permissionLevel = permissionLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
