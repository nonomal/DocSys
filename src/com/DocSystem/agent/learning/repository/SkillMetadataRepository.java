package com.DocSystem.agent.learning.repository;

import com.DocSystem.agent.learning.entity.SkillMetadata;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper interface for skill_metadata table.
 * XML: src/main/resources/mapper/SkillMetadataRepositoryMapper.xml
 */
public interface SkillMetadataRepository {

    int insert(SkillMetadata metadata);

    int updateById(SkillMetadata metadata);

    SkillMetadata findBySkillId(@Param("skillId") String skillId);

    List<SkillMetadata> findByCreatorId(@Param("creatorId") String creatorId);

    List<SkillMetadata> findAll();

    int deleteBySkillId(@Param("skillId") String skillId);
}
