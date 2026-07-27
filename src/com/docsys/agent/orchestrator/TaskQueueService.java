package com.docsys.agent.orchestrator;

import com.alibaba.fastjson.JSON;
import com.docsys.agent.entity.TaskEntity;
import com.docsys.agent.entity.TaskEntity.TaskStatus;
import com.docsys.agent.repository.TaskRepository;
import com.docsys.agent.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Task queue service with MySQL persistence for retry and recovery.
 * REL-03: Per D-09 — MySQL + state machine (pending/running/completed/failed)
 * REL-03: Per D-10 — Configurable timeout and max retries
 * REL-03: Per D-11 — Spring Boot startup scans and re-enqueues pending/running tasks
 *
 * Redis key prefix: queue:task:{taskId} (per D-05)
 */
@Service
public class TaskQueueService {

    private static final Logger log = LoggerFactory.getLogger(TaskQueueService.class);
    private static final String QUEUE_KEY_PREFIX = "queue:task:";

    @Value("${agent.task-retry.timeout-minutes:5}")
    private int timeoutMinutes;

    @Value("${agent.task-retry.max-retries:3}")
    private int maxRetries;

    @Autowired(required = false)
    private TaskRepository taskRepository;

    // Local executor for re-enqueueing recovered tasks
    private final Queue<String> pendingTaskQueue = new ConcurrentLinkedQueue<>();

    /**
     * Enqueue a new task.
     * Per D-09: Save to MySQL with pending status.
     */
    @Transactional
    public String enqueue(String taskType, String sessionId, Map<String, String> params, String metadataJson) {
        String taskId = UUID.randomUUID().toString();
        LocalDateTime timeoutAt = LocalDateTime.now().plusMinutes(timeoutMinutes);

        TaskEntity task = new TaskEntity()
                .withTaskId(taskId)
                .withSessionId(sessionId)
                .withTaskType(taskType)
                .withTaskParams(params != null ? JSON.toJSONString(params) : null)
                .withStatus(TaskStatus.PENDING)
                .withRetryCount(0)
                .withMaxRetries(maxRetries)
                .withTimeoutAt(timeoutAt)
                .withMetadata(metadataJson);

        task.setCreatedAt(LocalDateTime.now());
        taskRepository.insert(task);

        pendingTaskQueue.add(taskId);
        log.info("Task enqueued: taskId={}, type={}, timeoutAt={}", taskId, taskType, timeoutAt);
        return taskId;
    }

    /**
     * Mark task as running.
     * Per D-09: pending -> running transition.
     */
    @Transactional
    public void markRunning(String taskId) {
        TaskEntity task = taskRepository.selectByTaskId(taskId);
        if (task != null) {
            task.withStatus(TaskStatus.RUNNING)
                .withStartedAt(LocalDateTime.now())
                .withTimeoutAt(LocalDateTime.now().plusMinutes(timeoutMinutes));
            taskRepository.updateById(task);
            log.debug("Task marked running: taskId={}", taskId);
        }
    }

    /**
     * Mark task as completed.
     * Per D-09: running -> completed transition.
     * Filters emoji characters from result to prevent database charset issues.
     */
    @Transactional
    public void markCompleted(String taskId, String result) {
        TaskEntity task = taskRepository.selectByTaskId(taskId);
        if (task != null) {
            task.withStatus(TaskStatus.COMPLETED)
                .withResult(StringUtils.truncateAndFilter(result, 5000))
                .withCompletedAt(LocalDateTime.now());
            taskRepository.updateById(task);
            log.info("Task completed: taskId={}", taskId);
        }
    }

    /**
     * Mark task as failed, with retry logic.
     * Per D-09: running -> failed transition.
     * Per D-10: Increment retry count, re-enqueue if below max_retries.
     * Filters emoji characters from error messages to prevent database charset issues.
     */
    @Transactional
    public void markFailed(String taskId, String errorMessage) {
        TaskEntity task = taskRepository.selectByTaskId(taskId);
        if (task != null) {
            int newRetryCount = task.getRetryCount() + 1;
            String filteredError = StringUtils.truncateAndFilter(errorMessage, 2000);

            if (newRetryCount < task.getMaxRetries()) {
                // Per D-10: Retry — re-enqueue
                task.withStatus(TaskStatus.RETRY_PENDING)
                    .withRetryCount(newRetryCount)
                    .withErrorMessage(filteredError)
                    .withCompletedAt(LocalDateTime.now())
                    .withTimeoutAt(LocalDateTime.now().plusMinutes(timeoutMinutes));
                taskRepository.updateById(task);
                pendingTaskQueue.add(taskId);  // Re-add to pending queue
                log.warn("Task retry scheduled: taskId={}, retryCount={}/{}, error={}",
                        taskId, newRetryCount, task.getMaxRetries(), filteredError);
            } else {
                // Max retries exhausted
                task.withStatus(TaskStatus.FAILED)
                    .withRetryCount(newRetryCount)
                    .withErrorMessage(filteredError)
                    .withCompletedAt(LocalDateTime.now());
                taskRepository.updateById(task);
                log.error("Task failed permanently: taskId={}, retries exhausted, error={}",
                        taskId, filteredError);
            }
        }
    }

    /**
     * Resume pending/running tasks on startup.
     * Per D-11: Called by MainAgent @PostConstruct.
     * Per D-10: Tasks older than timeout-minutes are candidates for restart.
     */
    @Transactional(readOnly = true)
    public List<TaskEntity> getPendingOrRunningTasksForRecovery() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        String statuses = "'PENDING','RUNNING','RETRY_PENDING'";

        List<TaskEntity> tasks = taskRepository.selectPendingOrRunningOlderThan(statuses, cutoff);
        log.info("Found {} pending/running tasks older than {} minutes for recovery", tasks.size(), timeoutMinutes);
        return tasks;
    }

    /**
     * Re-enqueue a recovered task.
     */
    @Transactional
    public void reEnqueue(TaskEntity task) {
        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.withStatus(TaskStatus.FAILED)
                .withErrorMessage("Task timed out after all retry attempts during startup recovery")
                .withCompletedAt(LocalDateTime.now());
            taskRepository.updateById(task);
            log.warn("Task permanently failed during recovery (max retries): taskId={}", task.getTaskId());
            return;
        }

        // Per D-10: Increment retry count and re-enqueue
        task.withStatus(TaskStatus.RETRY_PENDING)
            .withRetryCount(task.getRetryCount() + 1)
            .withErrorMessage("Re-enqueued from startup recovery")
            .withTimeoutAt(LocalDateTime.now().plusMinutes(timeoutMinutes));
        taskRepository.updateById(task);
        pendingTaskQueue.add(task.getTaskId());
        log.info("Task re-enqueued from recovery: taskId={}, retryCount={}/{}",
                task.getTaskId(), task.getRetryCount(), task.getMaxRetries());
    }

    /**
     * Get task by ID.
     */
    public TaskEntity getTask(String taskId) {
        return taskRepository.selectByTaskId(taskId);
    }

    /**
     * Peek next pending task (FIFO).
     */
    public String peekPendingTask() {
        return pendingTaskQueue.poll();
    }
}