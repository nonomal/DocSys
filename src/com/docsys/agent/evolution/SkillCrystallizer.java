package com.docsys.agent.evolution;

import com.docsys.agent.skill.EnhancedSkill;
import com.docsys.agent.skill.EnhancedSkillManager;
import com.docsys.agent.skill.SkillCommand;
import com.docsys.agent.skill.SkillParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkillCrystallizer - Manages the skill crystallization lifecycle.
 *
 * Crystallization Flow:
 *   1. EvolutionTrigger.onTaskSuccess() is called after each successful task
 *   2. shouldCrystallize(taskType) checks if threshold (default 3) is met
 *   3. crystallize(taskType) generates the skill and registers it
 *
 * GA Self-Evolution Principles:
 * - Crystallization is idempotent: re-crystallizing the same taskType updates existing skill
 * - Runs asynchronously: never blocks user-facing task execution
 * - Backward compatible: only generates skills that don't override existing ones unless versioned
 */
@Component
public class SkillCrystallizer {

    private static final Logger log = LoggerFactory.getLogger(SkillCrystallizer.class);

    // Directory where generated skills are stored
    private static final String SKILLS_OUTPUT_PATH = "data/skills/";
    private static final String SOP_INPUT_PATH = "data/evolution/sops/";

    // Configurable crystallization threshold
    private int crystallizationThreshold = 3;

    // Track crystallization state in-memory (keyed by task type)
    private final Map<String, Integer> successCounts = new ConcurrentHashMap<>();

    private final ExperienceMemory experienceMemory;
    private final SOPGenerator sopGenerator;
    private final SkillVersionManager versionManager;

    @Autowired
    public SkillCrystallizer(ExperienceMemory experienceMemory,
                             SOPGenerator sopGenerator,
                             SkillVersionManager versionManager) {
        this.experienceMemory = experienceMemory;
        this.sopGenerator = sopGenerator;
        this.versionManager = versionManager;
    }

    @PostConstruct
    public void init() {
        // Ensure output directories exist
        try {
            Files.createDirectories(Paths.get(SKILLS_OUTPUT_PATH));
            Files.createDirectories(Paths.get(SOP_INPUT_PATH));
            log.info("SkillCrystallizer initialized. Threshold={}, SkillsOutput={}",
                crystallizationThreshold, SKILLS_OUTPUT_PATH);
        } catch (IOException e) {
            log.error("Failed to create skills directories", e);
        }
    }

    /**
     * Increment success count for a task type.
     * Called by EvolutionTrigger after each successful task.
     */
    public void recordSuccess(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) return;

        int count = successCounts.computeIfAbsent(taskType, k -> 0) + 1;
        successCounts.put(taskType, count);

        log.debug("Recorded success for taskType={}, count={}/{}",
            taskType, count, crystallizationThreshold);
    }

    /**
     * Check if a task type should be crystallized.
     * Returns true when success count reaches the configured threshold.
     */
    public boolean shouldCrystallize(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) return false;
        if (hasExistingSkill(taskType)) return false; // Don't override existing skills
        if (sopGenerator.hasSOP(taskType)) return false; // Already crystallized

        int count = successCounts.getOrDefault(taskType, 0);
        boolean ready = count >= crystallizationThreshold;

        if (ready) {
            log.info("Crystallization threshold reached for taskType={}: {}/{}",
                taskType, count, crystallizationThreshold);
        }

        return ready;
    }

    /**
     * Perform crystallization: generate skill.md and register with EnhancedSkillManager.
     *
     * @param taskType The task type to crystallize
     * @return The generated skill ID, or null if crystallization failed
     */
    public String crystallize(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) {
            log.warn("Cannot crystallize: taskType is null or blank");
            return null;
        }

        String skillId = taskType.toLowerCase().replace(" ", "_").trim();

        // Guard: don't override existing skills
        if (hasExistingSkill(taskType) && !isGeneratedSkill(taskType)) {
            log.info("Skipping crystallization for {}: a built-in/manual skill already exists", taskType);
            return null;
        }

        // Guard: don't re-crystallize if SOP already exists on disk
        if (sopGenerator.hasSOP(taskType)) {
            log.info("SOP already exists for {}, loading existing instead", taskType);
            return loadExistingSkill(taskType);
        }

        log.info("Starting crystallization for taskType={}", taskType);

        // Step 1: Gather sample queries from ExperienceMemory
        List<String> sampleQueries = gatherSampleQueries(taskType);
        if (sampleQueries.isEmpty()) {
            log.warn("No sample queries found for taskType={}, cannot generate SOP", taskType);
            return null;
        }

        // Step 2: Generate SOP
        String skillMdContent = sopGenerator.generateSOP(taskType, sampleQueries);
        if (skillMdContent == null || skillMdContent.trim().isEmpty()) {
            log.error("SOP generation returned empty content for {}", taskType);
            return null;
        }

        // Step 3: Write skill to disk
        String skillFilePath = writeSkillToDisk(skillId, skillMdContent);
        if (skillFilePath == null) {
            log.error("Failed to write skill {} to disk", skillId);
            return null;
        }

        // Step 4: Parse and register with EnhancedSkillManager
        EnhancedSkill skill = SkillParser.parse(skillMdContent, skillId);
        if (skill != null) {
            // Assign next version number
            int nextVersion = versionManager.getNextVersion(skillId);
            skill.setVersion(nextVersion + ".0.0");
            skill.setAuthor("DocSysAgent-SelfEvolution-v" + nextVersion);

            try {
                EnhancedSkillManager.getInstance().registerSkill(skill);
                log.info("Successfully crystallized skill: id={}, version={}, samples={}",
                    skillId, skill.getVersion(), sampleQueries.size());
            } catch (Exception e) {
                log.warn("EnhancedSkillManager registration failed for {}: {}",
                    skillId, e.getMessage());
            }
        }

        // Step 5: Record version
        versionManager.recordVersion(skillId, 1, 0.0);

        // Reset success count after crystallization
        successCounts.remove(taskType);

        return skillId;
    }

    /**
     * Gather sample queries for a task type from ExperienceMemory.
     */
    private List<String> gatherSampleQueries(String taskType) {
        List<String> samples = new ArrayList<>();

        // Get patterns from ExperienceMemory
        ExperienceMemory.IntentPattern pattern = experienceMemory.getPattern(taskType);
        if (pattern != null) {
            samples.addAll(pattern.sampleQueries);
        }

        // Also check the intent recognition index for this task type
        Collection<ExperienceMemory.IntentPattern> allPatterns = experienceMemory.getAllPatterns();
        for (ExperienceMemory.IntentPattern p : allPatterns) {
            if (p.intent.equalsIgnoreCase(taskType)) {
                samples.addAll(p.sampleQueries);
            }
        }

        // De-duplicate
        return new ArrayList<>(new LinkedHashSet<>(samples));
    }

    /**
     * Write skill.md content to disk.
     * Creates directory structure: data/skills/{skillId}/SKILL.md
     */
    private String writeSkillToDisk(String skillId, String content) {
        try {
            Path skillDir = Paths.get(SKILLS_OUTPUT_PATH, skillId);
            Files.createDirectories(skillDir);
            Path skillFile = skillDir.resolve("SKILL.md");
            Files.write(skillFile, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Wrote skill file: {}", skillFile);
            return skillFile.toString();
        } catch (IOException e) {
            log.error("Failed to write skill {} to disk: {}", skillId, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a built-in/manual skill already exists for this task type.
     * Protected so tests can override this to control isolation.
     */
    protected boolean hasExistingSkill(String taskType) {
        try {
            EnhancedSkill existing = EnhancedSkillManager.getInstance().getSkill(taskType);
            return existing != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a skill was previously generated by self-evolution.
     */
    private boolean isGeneratedSkill(String taskType) {
        String skillId = taskType.toLowerCase().replace(" ", "_").trim();
        Path skillDir = Paths.get(SKILLS_OUTPUT_PATH, skillId);
        return Files.exists(skillDir);
    }

    /**
     * Load an existing SOP skill from disk.
     */
    private String loadExistingSkill(String taskType) {
        String skillId = taskType.toLowerCase().replace(" ", "_").trim();
        Path sopFile = Paths.get(SOP_INPUT_PATH, skillId + ".md");

        if (!Files.exists(sopFile)) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(sopFile), StandardCharsets.UTF_8);
            EnhancedSkill skill = SkillParser.parse(content, skillId);
            if (skill != null) {
                EnhancedSkillManager.getInstance().registerSkill(skill);
                log.info("Loaded existing SOP skill: {}", skillId);
                return skillId;
            }
        } catch (IOException e) {
            log.error("Failed to load SOP for {}: {}", skillId, e.getMessage());
        }

        return null;
    }

    /**
     * Set the crystallization threshold (for testing or configuration).
     */
    public void setCrystallizationThreshold(int threshold) {
        this.crystallizationThreshold = Math.max(1, threshold);
    }

    /**
     * Get current success count for a task type.
     */
    public int getSuccessCount(String taskType) {
        return successCounts.getOrDefault(taskType, 0);
    }
}
