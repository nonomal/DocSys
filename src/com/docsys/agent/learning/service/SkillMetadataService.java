package com.docsys.agent.learning.service;

import com.docsys.agent.learning.entity.SkillMetadata;
import com.docsys.agent.learning.repository.SkillMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Skill metadata service. Implements user-level isolation:
 *  - PRIVATE skills: only creator
 *  - TENANT skills:  any logged-in user in the same tenant
 *  - PUBLIC skills:  anyone
 */
@Service
public class SkillMetadataService {

    private static final Logger log = LoggerFactory.getLogger(SkillMetadataService.class);

    @Autowired(required = false)
    private SkillMetadataRepository repository;

    public void saveMetadata(SkillMetadata metadata) {
        if (repository == null) {
            log.warn("SkillMetadataRepository is not available; skipping save");
            return;
        }
        Date now = new Date();
        if (metadata.getId() == null) {
            // new row
            SkillMetadata existing = repository.findBySkillId(metadata.getSkillId());
            if (existing != null) {
                metadata.setId(existing.getId());
                metadata.setCreatedAt(existing.getCreatedAt());
                metadata.setUpdatedAt(now);
                repository.updateById(metadata);
            } else {
                metadata.setCreatedAt(now);
                metadata.setUpdatedAt(now);
                repository.insert(metadata);
            }
        } else {
            metadata.setUpdatedAt(now);
            repository.updateById(metadata);
        }
    }

    public SkillMetadata getMetadata(String skillId) {
        if (repository == null) return null;
        return repository.findBySkillId(skillId);
    }

    public List<SkillMetadata> getVisibleSkills(String userId, String tenantId) {
        if (repository == null) return new ArrayList<>();
        List<SkillMetadata> all = repository.findAll();
        List<SkillMetadata> visible = new ArrayList<>();
        for (SkillMetadata m : all) {
            if (isVisible(m, userId, tenantId)) {
                visible.add(m);
            }
        }
        return visible;
    }

    public void deleteMetadata(String skillId) {
        if (repository == null) return;
        repository.deleteBySkillId(skillId);
    }

    /**
     * Determine whether a user is admin in their tenant (admins can manage
     * visibility on any tenant skill; non-admins can only manage their own).
     */
    public boolean isAdmin(String userId, String tenantId) {
        // Simple heuristic: if the user is admin upload, treat as admin.
        // Real implementation should query the user role table; kept minimal here
        // so the controller does not blow up on missing dependency.
        if (userId == null) return false;
        if (repository == null) return false;
        List<SkillMetadata> mine = repository.findByCreatorId(userId);
        for (SkillMetadata m : mine) {
            if (Boolean.TRUE.equals(m.getIsAdminSkill())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update visibility + optional allowed-user list for a skill.
     */
    public void updateVisibility(String skillId,
                                 String visibility,
                                 String allowedUserIds,
                                 String tenantId) {
        if (repository == null) return;
        SkillMetadata m = repository.findBySkillId(skillId);
        if (m == null) return;
        if (visibility != null) {
            try {
                m.setVisibility(com.docsys.agent.learning.entity.SkillMetadata.Visibility.valueOf(visibility));
            } catch (IllegalArgumentException ignored) { /* keep existing */ }
        }
        m.setAllowedUserIds(allowedUserIds);
        m.setTenantId(tenantId);
        m.setUpdatedAt(new Date());
        repository.updateById(m);
    }

    private boolean isVisible(SkillMetadata m, String userId, String tenantId) {
        SkillMetadata.Visibility v = m.getVisibilityEnum();
        if (v == null) return false;
        switch (v) {
            case PUBLIC:
                return true;
            case TENANT:
                return tenantId != null && tenantId.equals(m.getTenantId());
            case PRIVATE:
                return userId != null && userId.equals(m.getCreatorId());
            default:
                return false;
        }
    }
}
