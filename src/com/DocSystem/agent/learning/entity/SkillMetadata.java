package com.DocSystem.agent.learning.entity;

import java.util.Date;

/**
 * Skill metadata for user-level isolation and visibility control.
 * Java 8 compatible (no Lombok, no record — manually implemented builder).
 *
 * Schema: see src/main/resources/db/migration/V5__create_skill_metadata.sql
 * Mapper: src/main/resources/mapper/SkillMetadataRepositoryMapper.xml
 */
public class SkillMetadata {

    public enum Visibility {
        PRIVATE, TENANT, PUBLIC
    }

    private Long id;
    private String skillId;
    private String creatorId;
    private String creatorName;
    private Visibility visibility = Visibility.PRIVATE;
    private String allowedUserIds;   // JSON array
    private Boolean isAdminSkill = false;
    private String tenantId;
    private String name;
    private String category;
    private String version;
    private String filePath;
    private Date createdAt;
    private Date updatedAt;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public Visibility getVisibilityEnum() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    /** String form (for callers that use String visibility labels). */
    public String getVisibility() { return visibility == null ? null : visibility.name(); }
    public void setVisibility(String v) {
        if (v == null) { this.visibility = null; return; }
        try { this.visibility = Visibility.valueOf(v); }
        catch (IllegalArgumentException e) { /* keep default */ }
    }

    public String getAllowedUserIds() { return allowedUserIds; }
    public void setAllowedUserIds(String allowedUserIds) { this.allowedUserIds = allowedUserIds; }

    public Boolean getIsAdminSkill() { return isAdminSkill; }
    public void setIsAdminSkill(Boolean isAdminSkill) { this.isAdminSkill = isAdminSkill; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private final SkillMetadata m = new SkillMetadata();
        public Builder skillId(String v) { m.skillId = v; return this; }
        public Builder creatorId(String v) { m.creatorId = v; return this; }
        public Builder creatorName(String v) { m.creatorName = v; return this; }
        public Builder visibility(Visibility v) { m.visibility = v; return this; }
        public Builder visibility(String v) {
            if (v != null) {
                try { m.visibility = Visibility.valueOf(v); }
                catch (IllegalArgumentException ignored) { /* keep default */ }
            }
            return this;
        }
        public Builder allowedUserIds(String v) { m.allowedUserIds = v; return this; }
        public Builder isAdminSkill(Boolean v) { m.isAdminSkill = v; return this; }
        public Builder tenantId(String v) { m.tenantId = v; return this; }
        public Builder name(String v) { m.name = v; return this; }
        public Builder category(String v) { m.category = v; return this; }
        public Builder version(String v) { m.version = v; return this; }
        public Builder filePath(String v) { m.filePath = v; return this; }
        public SkillMetadata build() { return m; }
    }
}
