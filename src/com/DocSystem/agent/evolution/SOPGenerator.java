package com.DocSystem.agent.evolution;

import com.DocSystem.agent.skill.EnhancedSkill;
import com.DocSystem.agent.skill.SkillCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

/**
 * SOPGenerator - Extracts Standard Operating Procedures from successful task executions
 * and generates skill.md format content ready for crystallization.
 *
 * GA Self-Evolution Flow:
 *   1. ExperienceMemory.recordExecution() accumulates success patterns
 *   2. SOPGenerator.analyzePatterns() extracts task type + parameter -> outcome mappings
 *   3. SkillCrystallizer converts SOPs into executable skills
 *   4. EnhancedSkillManager registers new skills for future use
 *
 * Design:
 * - Immutable pattern extraction (no mutation of ExperienceMemory state)
 * - Template-based generation for consistent SKILL.md format
 * - Async-safe (no shared mutable state)
 */
@Component
public class SOPGenerator {

    private static final Logger log = LoggerFactory.getLogger(SOPGenerator.class);

    private static final String SOP_STORAGE_PATH = "data/evolution/sops/";

    // Default category heuristics — LinkedHashMap preserves insertion order so
    // more-specific keys are checked first (order matters: longer/more-specific keys first)
    private static final Map<String, String> CATEGORY_KEYWORDS = new java.util.LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put("repo", "repository");
        CATEGORY_KEYWORDS.put("search", "search");
        CATEGORY_KEYWORDS.put("doc", "document");
        CATEGORY_KEYWORDS.put("chat", "ai");
        CATEGORY_KEYWORDS.put("help", "system");
        CATEGORY_KEYWORDS.put("user", "user");
        CATEGORY_KEYWORDS.put("init", "system");
        CATEGORY_KEYWORDS.put("upload", "document");
        CATEGORY_KEYWORDS.put("download", "document");
        CATEGORY_KEYWORDS.put("delete", "document");
    }

    /**
     * Generate skill.md content from a task type and sample queries.
     *
     * @param taskType      The task type identifier (e.g., "list_repos", "search_doc")
     * @param sampleQueries List of user queries that triggered this task successfully
     * @return Full SKILL.md format content ready to be written to disk
     */
    public String generateSOP(String taskType, List<String> sampleQueries) {
        if (taskType == null || taskType.trim().isEmpty()) {
            log.warn("Cannot generate SOP: taskType is null or blank");
            return null;
        }
        if (sampleQueries == null || sampleQueries.isEmpty()) {
            log.warn("Cannot generate SOP for {}: no sample queries", taskType);
            return null;
        }

        String skillId = deriveSkillId(taskType);
        String name = deriveSkillName(taskType);
        String category = deriveCategory(taskType);
        String version = "1.0.0";

        List<String> triggers = deriveTriggers(sampleQueries);
        List<SkillCommand> commands = deriveCommands(taskType, sampleQueries);
        String executionFlow = deriveExecutionFlow(taskType, sampleQueries);
        List<String> examples = deriveExamples(sampleQueries);

        EnhancedSkill skill = new EnhancedSkill();
        skill.setId(skillId);
        skill.setName(name);
        skill.setDescription(buildDescription(taskType, name, sampleQueries.size()));
        skill.setCategory(category);
        skill.setVersion(version);
        skill.setAuthor("DocSysAgent-SelfEvolution");
        skill.setExecutionFlow(executionFlow);

        triggers.forEach(skill::addTrigger);
        commands.forEach(skill::addCommand);
        examples.forEach(skill::addExample);

        String skillMd = skill.toSkillMarkdown();
        log.info("Generated SOP for taskType={}, skillId={}, triggerCount={}",
            taskType, skillId, triggers.size());

        // Persist SOP to disk asynchronously
        persistSOPAsync(skillId, skillMd);

        return skillMd;
    }

    /**
     * Derive a skill ID from task type.
     * e.g., "list_repos" -> "list_repos", "search_doc" -> "search_doc"
     */
    private String deriveSkillId(String taskType) {
        return taskType.toLowerCase().replace(" ", "_").trim();
    }

    /**
     * Derive a human-readable skill name.
     * e.g., "list_repos" -> "List Repositories"
     */
    private String deriveSkillName(String taskType) {
        String[] parts = taskType.toLowerCase().split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Derive the skill category from task type keywords.
     */
    private String deriveCategory(String taskType) {
        String lower = taskType.toLowerCase();
        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "system";
    }

    /**
     * Extract trigger phrases from sample queries.
     * Uses the most common n-grams (2-5 words) as triggers.
     */
    private List<String> deriveTriggers(List<String> queries) {
        Map<String, Integer> ngramCounts = new HashMap<>();

        for (String query : queries) {
            String lower = query.toLowerCase().trim();
            // Extract 2-5 word phrases
            String[] words = lower.split("\\s+");
            for (int n = 2; n <= Math.min(5, words.length); n++) {
                for (int i = 0; i <= words.length - n; i++) {
                    StringBuilder ngram = new StringBuilder();
                    for (int j = i; j < i + n; j++) {
                        if (j > i) ngram.append(" ");
                        ngram.append(words[j]);
                    }
                    ngramCounts.merge(ngram.toString(), 1, Integer::sum);
                }
            }
        }

        // Sort by frequency and take top triggers
        return ngramCounts.entrySet().stream()
            .filter(e -> e.getValue() >= Math.max(1, queries.size() / 3)) // At least 1 or 1/3 of samples
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Derive CLI command templates from task type and sample queries.
     */
    private List<SkillCommand> deriveCommands(String taskType, List<String> queries) {
        List<SkillCommand> commands = new ArrayList<>();

        // Primary command from task type
        SkillCommand primary = new SkillCommand();
        String cliCommand = taskType.replace("_", "-");
        primary.setPattern(cliCommand);
        primary.setCliCommand(cliCommand);
        primary.setDescription("Generated from self-evolution for task: " + taskType);
        commands.add(primary);

        // Extract command-like patterns from sample queries
        Set<String> seen = new HashSet<>();
        seen.add(cliCommand);
        Pattern cmdPattern = Pattern.compile("^([a-z]+(?:[-][a-z]+)?)\\s+");

        for (String query : queries) {
            Matcher m = cmdPattern.matcher(query.toLowerCase().trim());
            if (m.find()) {
                String extracted = m.group(1);
                if (!seen.contains(extracted)) {
                    seen.add(extracted);
                    SkillCommand cmd = new SkillCommand();
                    cmd.setPattern(extracted);
                    cmd.setCliCommand(extracted);
                    cmd.setDescription("Auto-detected from user query: " + query);
                    commands.add(cmd);
                    if (commands.size() >= 5) break; // Limit to 5 commands
                }
            }
        }

        return commands;
    }

    /**
     * Derive execution flow from task type.
     */
    private String deriveExecutionFlow(String taskType, List<String> queries) {
        String lower = taskType.toLowerCase();

        if (lower.contains("list") && (lower.contains("repo") || lower.contains("doc"))) {
            return "1. Call DocSys API to list resources\n" +
                   "2. Parse JSON response\n" +
                   "3. Format as readable list\n" +
                   "4. Return to user";
        } else if (lower.contains("search")) {
            return "1. Extract search query from user input\n" +
                   "2. Call DocSys search API\n" +
                   "3. Parse and rank results\n" +
                   "4. Return formatted results to user";
        } else if (lower.contains("create") || lower.contains("add")) {
            return "1. Validate required parameters\n" +
                   "2. Call DocSys create API\n" +
                   "3. Handle response (success/error)\n" +
                   "4. Return result to user";
        } else if (lower.contains("delete")) {
            return "1. Confirm deletion with user\n" +
                   "2. Call DocSys delete API\n" +
                   "3. Return confirmation to user";
        } else if (lower.contains("chat")) {
            return "1. Send user message to LLM service\n" +
                   "2. Receive and format AI response\n" +
                   "3. Return to user";
        } else {
            return "1. Parse user intent from query\n" +
                   "2. Execute appropriate DocSys API call\n" +
                   "3. Format and return result";
        }
    }

    /**
     * Derive usage examples from sample queries.
     */
    private List<String> deriveExamples(List<String> queries) {
        return queries.stream()
            .limit(5)
            .map(String::trim)
            .filter(q -> !q.isEmpty())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Build a description for the skill.
     */
    private String buildDescription(String taskType, String name, int sampleCount) {
        return String.format(
            "Auto-generated skill for task type '%s'. " +
            "Discovered through %d successful executions. " +
            "Handles %s operations in DocSystem.",
            taskType, sampleCount, name.toLowerCase()
        );
    }

    /**
     * Persist SOP to disk asynchronously.
     * File path: data/evolution/sops/{skillId}.md
     */
    private void persistSOPAsync(String skillId, String skillMd) {
        Thread t = new Thread(() -> {
            try {
                java.nio.file.Path dir = java.nio.file.Paths.get(SOP_STORAGE_PATH);
                if (!java.nio.file.Files.exists(dir)) {
                    java.nio.file.Files.createDirectories(dir);
                }
                java.nio.file.Path file = dir.resolve(skillId + ".md");
                java.nio.file.Files.write(file, skillMd.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                log.debug("Persisted SOP to {}", file);
            } catch (Exception e) {
                log.warn("Failed to persist SOP for {}: {}", skillId, e.getMessage());
            }
        }, "SOPGenerator-persist-" + skillId);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Check if a skill with the given task type already has a SOP on disk.
     */
    public boolean hasSOP(String taskType) {
        String skillId = deriveSkillId(taskType);
        return java.nio.file.Files.exists(
            java.nio.file.Paths.get(SOP_STORAGE_PATH, skillId + ".md"));
    }
}
