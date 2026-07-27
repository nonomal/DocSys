package com.docsys.agent.evolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EvolutionTrigger - Integration point for self-evolution in task execution flow.
 *
 * Responsibilities:
 * 1. Hook into task execution lifecycle (after successful SubAgent.execute())
 * 2. Track success counts per task type
 * 3. Trigger crystallization asynchronously when threshold is reached
 *
 * GA Self-Evolution Integration:
 * - Called from MainAgent after each successful task execution
 * - Runs on a separate async thread pool (never blocks user-facing execution)
 * - Uses SkillCrystallizer to generate and register new skills
 *
 * CRITICAL: All evolution logic runs ASYNCHRONOUSLY.
 * This component must NEVER block the main task execution thread.
 */
@Component
public class EvolutionTrigger {

    private static final Logger log = LoggerFactory.getLogger(EvolutionTrigger.class);

    private final SkillCrystallizer crystallizer;
    private final SkillVersionManager versionManager;

    // Track which skills have already been crystallized (prevent duplicate attempts)
    private final Map<String, Boolean> crystallizedTasks = new ConcurrentHashMap<>();

    @Autowired
    public EvolutionTrigger(SkillCrystallizer crystallizer,
                           SkillVersionManager versionManager) {
        this.crystallizer = crystallizer;
        this.versionManager = versionManager;
        log.info("EvolutionTrigger initialized with async crystallization support");
    }

    /**
     * Called after each successful task execution.
     * This method is async - it returns immediately and runs in background.
     *
     * @param taskType   The type of task that succeeded (e.g., "list_repos", "search_doc")
     * @param userQuery  The original user query
     * @param params     Task parameters (for potential future skill refinement)
     */
    @Async
    public void onTaskSuccess(String taskType, String userQuery, Map<String, String> params) {
        if (taskType == null || taskType.trim().isEmpty()) {
            log.debug("onTaskSuccess called with null/blank taskType, skipping");
            return;
        }

        // Skip already crystallized tasks
        if (crystallizedTasks.getOrDefault(taskType, false)) {
            log.trace("Task {} already crystallized, skipping", taskType);
            return;
        }

        try {
            log.debug("Processing evolution trigger for taskType={}, query={}", taskType, userQuery);

            // Step 1: Record success in SkillCrystallizer (increments counter)
            crystallizer.recordSuccess(taskType);

            // Step 2: Check if crystallization threshold is reached
            if (crystallizer.shouldCrystallize(taskType)) {
                // Step 3: Trigger crystallization (generates and registers skill)
                String skillId = crystallizer.crystallize(taskType);

                if (skillId != null) {
                    crystallizedTasks.put(taskType, true);
                    log.info("Evolution SUCCESS: Task {} crystallized into skill {}",
                        taskType, skillId);
                } else {
                    log.warn("Evolution FAILED: crystallization returned null for {}", taskType);
                }
            } else {
                int currentCount = crystallizer.getSuccessCount(taskType);
                log.debug("Task {} is {} of {} successes — not yet ready for crystallization",
                    taskType, currentCount, 3);
            }

        } catch (Exception e) {
            // Never let evolution errors propagate to the main task flow
            log.error("Error in evolution trigger for taskType={}: {}", taskType, e.getMessage(), e);
        }
    }

    /**
     * Called after each failed task execution.
     * Currently logs the failure; can be extended for failure pattern analysis.
     */
    @Async
    public void onTaskFailure(String taskType, String userQuery, String errorMessage) {
        if (taskType == null) return;

        try {
            log.debug("EvolutionTrigger: task {} failed for query '{}': {}",
                taskType, userQuery, errorMessage);
            // Future: Could implement failure pattern analysis here
            // e.g., update failureStrategies in ExperienceMemory
        } catch (Exception e) {
            log.warn("Error in onTaskFailure: {}", e.getMessage());
        }
    }

    /**
     * Record a successful execution directly from ExperienceMemory.
     * This overload uses ExperienceMemory directly for success pattern tracking.
     */
    @Async
    public void onExecutionSuccess(String taskType, String userQuery, boolean success, long duration) {
        if (taskType == null) return;

        try {
            ExperienceMemory memory = ExperienceMemory.getInstance();
            memory.recordExecution(userQuery, taskType, success, duration, null);

            if (success) {
                onTaskSuccess(taskType, userQuery, null);
            }
        } catch (Exception e) {
            log.warn("Error recording execution success for {}: {}", taskType, e.getMessage());
        }
    }

    /**
     * Check if a task type has already been crystallized.
     */
    public boolean isCrystallized(String taskType) {
        return crystallizedTasks.getOrDefault(taskType, false);
    }

    /**
     * Get count of successfully crystallized tasks.
     */
    public int getCrystallizedCount() {
        return (int) crystallizedTasks.values().stream().filter(Boolean::booleanValue).count();
    }

    /**
     * Reset crystallized state (for testing only).
     */
    public void resetCrystallizedState(String taskType) {
        crystallizedTasks.remove(taskType);
    }
}
