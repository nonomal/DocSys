package com.DocSystem.agent.evolution;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * SkillVersionManager - Tracks skill version history for self-evolved skills.
 *
 * Each skill version record contains:
 * - skillName: The skill identifier
 * - version: Version number (major.minor.patch)
 * - timestamp: When the version was created
 * - triggerCount: How many times this skill was triggered
 * - avgSuccessRate: Average success rate across all executions
 *
 * Storage: data/evolution/versions/{skillName}/versions.json
 * Per-version files: data/evolution/versions/{skillName}/v{major}.json
 */
@Component
public class SkillVersionManager {

    private static final Logger log = LoggerFactory.getLogger(SkillVersionManager.class);

    private static final String VERSIONS_PATH = "data/evolution/versions/";

    // In-memory cache of skill versions
    private final Map<String, List<SkillVersion>> versionHistory = new ConcurrentHashMap<>();
    private final ExecutorService diskExecutor;

    public SkillVersionManager() {
        this.diskExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkillVersionManager-DiskWriter");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void init() {
        loadAllVersions();
        log.info("SkillVersionManager initialized with {} tracked skills", versionHistory.size());
    }

    /**
     * Record a new version for a skill.
     *
     * @param skillName    Skill identifier
     * @param majorVersion Major version number (auto-incremented)
     * @param successRate  Current success rate (0.0 - 1.0)
     */
    public void recordVersion(String skillName, int majorVersion, double successRate) {
        if (skillName == null || skillName.trim().isEmpty()) return;

        int nextVersion = getNextVersion(skillName);
        SkillVersion version = new SkillVersion();
        version.skillName = skillName;
        version.version = nextVersion + ".0.0";
        version.majorVersion = nextVersion;
        version.timestamp = Instant.now().toEpochMilli();
        version.triggerCount = 0;
        version.avgSuccessRate = successRate;

        versionHistory.computeIfAbsent(skillName, k -> new CopyOnWriteArrayList<>()).add(version);

        // Persist asynchronously
        persistVersionAsync(skillName, version);

        log.debug("Recorded version {} for skill {}", version.version, skillName);
    }

    /**
     * Increment the trigger count for the latest version of a skill.
     */
    public void recordTrigger(String skillName) {
        if (skillName == null) return;

        List<SkillVersion> versions = versionHistory.get(skillName);
        if (versions == null || versions.isEmpty()) return;

        // Update the latest version (last in list)
        SkillVersion latest = versions.get(versions.size() - 1);
        latest.triggerCount++;
        latest.avgSuccessRate = calculateSuccessRate(skillName);

        // Async persist
        persistVersionAsync(skillName, latest);
    }

    /**
     * Get the next version number for a skill.
     *
     * @return Next major version number (1-based)
     */
    public int getNextVersion(String skillName) {
        List<SkillVersion> versions = versionHistory.get(skillName);
        if (versions == null || versions.isEmpty()) {
            return 1;
        }
        return versions.stream()
            .mapToInt(v -> v.majorVersion)
            .max()
            .orElse(0) + 1;
    }

    /**
     * Get the latest version record for a skill.
     */
    public Optional<SkillVersion> getLatestVersion(String skillName) {
        if (skillName == null) return Optional.empty();
        List<SkillVersion> versions = versionHistory.get(skillName);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.get(versions.size() - 1));
    }

    /**
     * Get all version records for a skill.
     */
    public List<SkillVersion> getVersionHistory(String skillName) {
        return versionHistory.getOrDefault(skillName, Collections.emptyList());
    }

    /**
     * Get all tracked skills.
     */
    public Set<String> getAllTrackedSkills() {
        return new HashSet<>(versionHistory.keySet());
    }

    /**
     * Calculate the success rate for a skill based on ExperienceMemory.
     */
    private double calculateSuccessRate(String skillName) {
        try {
            ExperienceMemory memory = ExperienceMemory.getInstance();
            ExperienceMemory.IntentPattern pattern = memory.getPattern(skillName);
            if (pattern != null && pattern.successCount > 0) {
                // Use a simple heuristic: higher success count = higher rate
                return Math.min(1.0, pattern.successCount / 10.0);
            }
        } catch (Exception e) {
            log.debug("Could not calculate success rate for {}: {}", skillName, e.getMessage());
        }
        return 0.0;
    }

    /**
     * Persist a single version to disk asynchronously.
     */
    private void persistVersionAsync(String skillName, SkillVersion version) {
        if (diskExecutor.isShutdown()) return;
        diskExecutor.submit(() -> {
            try {
                persistVersion(skillName, version);
            } catch (Exception e) {
                log.warn("Failed to persist version for {}: {}", skillName, e.getMessage());
            }
        });
    }

    /**
     * Persist a version record to disk.
     * File: data/evolution/versions/{skillName}/v{major}.json
     */
    private void persistVersion(String skillName, SkillVersion version) throws IOException {
        Path skillDir = Paths.get(VERSIONS_PATH, skillName);
        Files.createDirectories(skillDir);

        Path versionFile = skillDir.resolve("v" + version.majorVersion + ".json");
        String json = JSON.toJSONString(version);
        Files.write(versionFile, json.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Also update the index file
        updateIndexFile(skillDir, skillName);

        log.debug("Persisted version {} for skill {} to {}", version.version, skillName, versionFile);
    }

    /**
     * Update the index file for a skill's version history.
     */
    private void updateIndexFile(Path skillDir, String skillName) throws IOException {
        List<SkillVersion> versions = versionHistory.getOrDefault(skillName, Collections.emptyList());
        Path indexFile = skillDir.resolve("versions.json");
        String json = JSON.toJSONString(versions);
        Files.write(indexFile, json.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Load all version records from disk on startup.
     */
    private void loadAllVersions() {
        try {
            Path baseDir = Paths.get(VERSIONS_PATH);
            if (!Files.exists(baseDir)) return;

            Files.list(baseDir).filter(Files::isDirectory).forEach(skillDir -> {
                String skillName = skillDir.getFileName().toString();
                Path indexFile = skillDir.resolve("versions.json");

                if (Files.exists(indexFile)) {
                    try {
                        String json = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
                        List<SkillVersion> versions = JSON.parseObject(json,
                            new TypeReference<List<SkillVersion>>() {});
                        if (versions != null) {
                            versionHistory.put(skillName, new CopyOnWriteArrayList<>(versions));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load version history for {}: {}", skillName, e.getMessage());
                    }
                }
            });

            log.info("Loaded version history for {} skills from disk", versionHistory.size());
        } catch (IOException e) {
            log.warn("Failed to load version history: {}", e.getMessage());
        }
    }

    // ==================== Inner Classes ====================

    /**
     * SkillVersion - Version record for a self-evolved skill.
     */
    public static class SkillVersion implements Serializable {
        public String skillName;
        public String version;        // Full version string: "1.0.0"
        public int majorVersion;       // Major version number
        public long timestamp;        // Unix epoch millis
        public int triggerCount;       // Number of times this version was triggered
        public double avgSuccessRate; // 0.0 - 1.0

        @Override
        public String toString() {
            return String.format("SkillVersion{name=%s, version=%s, triggers=%d, successRate=%.2f}",
                skillName, version, triggerCount, avgSuccessRate);
        }
    }
}
