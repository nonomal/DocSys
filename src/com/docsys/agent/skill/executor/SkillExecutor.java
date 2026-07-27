package com.docsys.agent.skill.executor;

import com.docsys.agent.core.AgentContext;

import java.util.Map;

/**
 * SkillExecutor - pluggable executor interface for skill execution.
 *
 * Spring auto-collects all implementations via List&lt;SkillExecutor&gt;.
 * Executors are sorted by {@code @Order} ascending; the first matching
 * executor (lowest order value) handles each skill.
 *
 * <p>Typical ordering:
 * <ul>
 *   <li>SkillExecutorRegistry: @Order(0) — highest priority, registry itself</li>
 *   <li>DocSysSkillExecutor:   @Order(100) — built-in skills</li>
 *   <li>ExternalSkillExecutor: @Order(200) — external skills from disk</li>
 * </ul>
 */
public interface SkillExecutor {

    /**
     * Priority for executor selection (lower = higher priority).
     * Use {@code @Order} annotation on the implementing class.
     */
    int getOrder();

    /**
     * Check if this executor can handle the given skill identifier.
     *
     * @param skillId skill identifier (e.g. "list_repos", "ant-expert")
     * @return true if this executor can execute this skill
     */
    boolean canHandle(String skillId);

    /**
     * Execute the skill with the given parameters.
     *
     * @param skillId skill identifier
     * @param params  skill parameters from the task
     * @param context current agent context (may be null for simple executors)
     * @return execution result; never null
     */
    SkillExecutionResult execute(String skillId, Map<String, String> params, AgentContext context);
}
