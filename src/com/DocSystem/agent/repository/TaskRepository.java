package com.DocSystem.agent.repository;

import com.DocSystem.agent.entity.TaskEntity;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Task repository for retry queue persistence.
 * REL-03: Provides persistence operations for agent task state machine.
 * MyBatis mapper interface.
 */
public interface TaskRepository {

    TaskEntity selectByTaskId(@Param("taskId") String taskId);

    List<TaskEntity> selectBySessionId(@Param("sessionId") String sessionId);

    List<TaskEntity> selectByStatus(@Param("status") String status);

    List<TaskEntity> selectPendingOrRunningOlderThan(
            @Param("statuses") String statuses,
            @Param("cutoff") LocalDateTime cutoff);

    List<TaskEntity> selectTimedOutTasks(
            @Param("statuses") String statuses,
            @Param("now") LocalDateTime now);

    long countRetryableTasks(
            @Param("sessionId") String sessionId,
            @Param("status") String status);

    int updateTaskStatus(
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("retryCount") int retryCount,
            @Param("errorMsg") String errorMessage,
            @Param("completedAt") LocalDateTime completedAt);

    int deleteOldTasks(@Param("cutoff") LocalDateTime cutoff);

    int insert(TaskEntity task);

    int updateById(TaskEntity task);

    int deleteById(@Param("id") Long id);

    List<TaskEntity> selectAll(@Param("offset") int offset, @Param("limit") int limit);
}