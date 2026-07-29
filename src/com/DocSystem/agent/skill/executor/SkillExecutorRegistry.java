package com.DocSystem.agent.skill.executor;

import com.DocSystem.agent.core.AgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * SkillExecutorRegistry - auto-collects all SkillExecutor implementations via Spring
 * and dispatches skill execution to the highest-priority matching executor.
 *
 * <p>Executors are sorted by {@code @Order} ascending; the first executor whose
 * {@code canHandle(skillId)} returns true handles the skill.
 *
 * <p>Ordering convention:
 * <ul>
 *   <li>0-99:   Core / registry (this class @Order(0))</li>
 *   <li>100-199: Built-in skill executors (DocSysSkillExecutor @Order(100))</li>
 *   <li>200+:   External skill executors (ExternalSkillExecutor @Order(200))</li>
 * </ul>
 */
@Component
@Order(0)
public class SkillExecutorRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutorRegistry.class);

    /** Immutable snapshot of executors sorted by @Order ascending. */
    private final List<SkillExecutor> executors;

    @Autowired
    public SkillExecutorRegistry(List<SkillExecutor> executors) {
        // Defensive copy and sort at construction time — thread-safe read
        List<SkillExecutor> sorted = new ArrayList<>(executors);
        sorted.sort(Comparator.comparingInt(SkillExecutor::getOrder));
        this.executors = Collections.unmodifiableList(sorted);
        log.info("SkillExecutorRegistry initialized with {} executor(s): {}",
            executors.size(),
            executors.stream()
                .map(e -> e.getClass().getSimpleName() + "@" + e.getOrder())
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Find the highest-priority executor that can handle the given skillId.
     *
     * @param skillId skill identifier
     * @return matching executor, or null if none found
     */
    public SkillExecutor findExecutor(String skillId) {
        for (SkillExecutor executor : executors) {
            if (executor.canHandle(skillId)) {
                log.debug("Found executor {} for skill '{}'", executor.getClass().getSimpleName(), skillId);
                return executor;
            }
        }
        log.warn("No executor found for skill '{}'", skillId);
        return null;
    }

    /**
     * Execute a skill using the first matching executor.
     *
     * @param skillId skill identifier
     * @param params  skill parameters
     * @param context current agent context (may be null)
     * @return execution result, or an error result if no executor found
     */
    public SkillExecutionResult execute(String skillId, Map<String, String> params, AgentContext context) {
        SkillExecutor executor = findExecutor(skillId);
        if (executor == null) {
            return SkillExecutionResult.error("No executor found for skill: " + skillId);
        }
        try {
            return executor.execute(skillId, params, context);
        } catch (Exception e) {
            log.error("Executor {} failed for skill '{}'", executor.getClass().getSimpleName(), skillId, e);
            return SkillExecutionResult.error("Skill execution failed: " + e.getMessage());
        }
    }

    /**
     * Return all registered executors in priority order (for diagnostics).
     *
     * @return unmodifiable list of executors sorted by @Order ascending
     */
    public List<SkillExecutor> getAllExecutors() {
        return executors;
    }
}
