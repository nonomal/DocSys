package com.DocSystem.agent.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TaskPersistenceManager - Manages task persistence and state
 * 
 * Key Features (aligned with Claude Code Tasks):
 * 1. Persistent task storage (JSON files)
 * 2. Task states: PENDING, IN_PROGRESS, COMPLETED, FAILED, WAITING_DEPENDENCY
 * 3. Task dependencies (DAG support)
 * 4. Resume from checkpoint
 * 5. Task progress tracking
 * 
 * Tasks are saved to disk to survive restarts and enable:
 * - Long-running task execution
 * - Task recovery after failures
 * - Audit trail
 * - Parallel execution tracking
 */
@Deprecated
public class TaskPersistenceManager {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPersistenceManager.class);
    private static TaskPersistenceManager instance;
    
    private final Path tasksDirectory;
    private final Map<String, PersistedTask> taskCache;
    
    // Task states
    public enum TaskState {
        PENDING,           // Task created, not started
        QUEUED,            // Task queued for execution
        IN_PROGRESS,       // Currently executing
        WAITING_DEPENDENCY, // Waiting for dependent tasks
        COMPLETED,         // Successfully completed
        FAILED,            // Failed with error
        CANCELLED,         // Cancelled by user
        PAUSED             // Paused (can resume)
    }
    
    /**
     * PersistedTask - represents a task with full state
     */
    public static class PersistedTask implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String taskId;
        private String parentTaskId;           // Parent task (for decomposition)
        private List<String> dependentTaskIds; // Tasks that depend on this
        private List<String> prerequisiteTaskIds; // Tasks that must complete first
        
        private String skillId;                // Skill to execute
        private String command;                // Original user command
        private Map<String, Object> parameters;
        
        private TaskState state;
        private String currentAttempt;          // Current execution attempt ID
        private int maxRetries;
        private int currentRetries;
        
        private String result;                 // Execution result
        private String errorMessage;           // Error if failed
        private Double progress;              // 0.0 - 1.0
        
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime lastUpdatedAt;
        
        private Map<String, Object> metadata; // Additional data
        
        public PersistedTask() {
            this.taskId = UUID.randomUUID().toString();
            this.state = TaskState.PENDING;
            this.parameters = new HashMap<>();
            this.dependentTaskIds = new ArrayList<>();
            this.prerequisiteTaskIds = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.maxRetries = 3;
            this.currentRetries = 0;
            this.createdAt = LocalDateTime.now();
            this.lastUpdatedAt = LocalDateTime.now();
        }
        
        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private final PersistedTask task = new PersistedTask();
            
            public Builder taskId(String taskId) { task.taskId = taskId; return this; }
            public Builder skillId(String skillId) { task.skillId = skillId; return this; }
            public Builder command(String command) { task.command = command; return this; }
            public Builder parameters(Map<String, Object> params) { task.parameters = params; return this; }
            public Builder parentTaskId(String parentId) { task.parentTaskId = parentId; return this; }
            public Builder addPrerequisite(String taskId) { 
                task.prerequisiteTaskIds.add(taskId); return this; 
            }
            
            public PersistedTask build() {
                return task;
            }
        }
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getParentTaskId() { return parentTaskId; }
        public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }
        
        public List<String> getDependentTaskIds() { return dependentTaskIds; }
        public void addDependentTaskId(String id) { this.dependentTaskIds.add(id); }
        
        public List<String> getPrerequisiteTaskIds() { return prerequisiteTaskIds; }
        public void addPrerequisiteTaskId(String id) { this.prerequisiteTaskIds.add(id); }
        
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public TaskState getState() { return state; }
        public void setState(TaskState state) { 
            this.state = state; 
            this.lastUpdatedAt = LocalDateTime.now();
        }
        
        public String getCurrentAttempt() { return currentAttempt; }
        public void setCurrentAttempt(String currentAttempt) { this.currentAttempt = currentAttempt; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public int getCurrentRetries() { return currentRetries; }
        public void incrementRetries() { this.currentRetries++; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Double getProgress() { return progress; }
        public void setProgress(Double progress) { this.progress = progress; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        
        public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
        
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    private TaskPersistenceManager() {
        // Initialize tasks directory
        String userDir = System.getProperty("user.dir", ".");
        this.tasksDirectory = Paths.get(userDir, "data", "tasks");
        
        try {
            Files.createDirectories(tasksDirectory);
            log.info("Task persistence directory: {}", tasksDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create tasks directory", e);
        }
        
        this.taskCache = new ConcurrentHashMap<>();
        
        // Load existing tasks
        loadExistingTasks();
    }
    
    public static synchronized TaskPersistenceManager getInstance() {
        if (instance == null) {
            instance = new TaskPersistenceManager();
        }
        return instance;
    }
    
    /**
     * Load existing tasks from disk
     */
    private void loadExistingTasks() {
        try {
            Files.list(tasksDirectory)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        PersistedTask task = deserializeTask(content);
                        if (task != null) {
                            taskCache.put(task.getTaskId(), task);
                            log.debug("Loaded task: {} from {}", task.getTaskId(), path);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load task from {}: {}", path, e.getMessage());
                    }
                });
            log.info("Loaded {} existing tasks", taskCache.size());
        } catch (IOException e) {
            log.error("Failed to list tasks directory", e);
        }
    }
    
    /**
     * Create a new task
     */
    public PersistedTask createTask(String skillId, String command, Map<String, Object> parameters) {
        PersistedTask task = PersistedTask.builder()
            .skillId(skillId)
            .command(command)
            .parameters(parameters)
            .build();
        
        saveTask(task);
        taskCache.put(task.getTaskId(), task);
        
        log.info("Created task: {} for skill: {}", task.getTaskId(), skillId);
        return task;
    }
    
    /**
     * Create a task with dependencies
     */
    public PersistedTask createTask(String skillId, String command, Map<String, Object> parameters,
                                    List<String> prerequisites) {
        PersistedTask task = PersistedTask.builder()
            .skillId(skillId)
            .command(command)
            .parameters(parameters)
            .build();
        
        if (prerequisites != null) {
            for (String prereqId : prerequisites) {
                task.addPrerequisiteTaskId(prereqId);
            }
        }
        
        saveTask(task);
        taskCache.put(task.getTaskId(), task);
        
        // Update dependent tasks
        if (prerequisites != null) {
            for (String prereqId : prerequisites) {
                PersistedTask prereq = taskCache.get(prereqId);
                if (prereq != null) {
                    prereq.addDependentTaskId(task.getTaskId());
                    saveTask(prereq);
                }
            }
        }
        
        log.info("Created task: {} with {} prerequisites", task.getTaskId(), 
                prerequisites != null ? prerequisites.size() : 0);
        return task;
    }
    
    /**
     * Get task by ID
     */
    public PersistedTask getTask(String taskId) {
        return taskCache.get(taskId);
    }
    
    /**
     * Get all tasks
     */
    public List<PersistedTask> getAllTasks() {
        return new ArrayList<>(taskCache.values());
    }
    
    /**
     * Get tasks by state
     */
    public List<PersistedTask> getTasksByState(TaskState state) {
        return taskCache.values().stream()
            .filter(t -> t.getState() == state)
            .collect(Collectors.toList());
    }
    
    /**
     * Get tasks ready to execute (PENDING with all prerequisites COMPLETED)
     */
    public List<PersistedTask> getReadyTasks() {
        return taskCache.values().stream()
            .filter(t -> t.getState() == TaskState.PENDING)
            .filter(t -> {
                // Check all prerequisites are completed
                for (String prereqId : t.getPrerequisiteTaskIds()) {
                    PersistedTask prereq = taskCache.get(prereqId);
                    if (prereq == null || prereq.getState() != TaskState.COMPLETED) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Update task state
     */
    public void updateTaskState(String taskId, TaskState newState) {
        PersistedTask task = taskCache.get(taskId);
        if (task != null) {
            task.setState(newState);
            
            if (newState == TaskState.IN_PROGRESS) {
                task.setStartedAt(LocalDateTime.now());
            } else if (newState == TaskState.COMPLETED || newState == TaskState.FAILED) {
                task.setCompletedAt(LocalDateTime.now());
            }
            
            saveTask(task);
            
            // Check dependent tasks
            if (newState == TaskState.COMPLETED) {
                checkDependentTasks(taskId);
            }
            
            log.info("Task {} state updated to {}", taskId, newState);
        }
    }
    
    /**
     * Check if dependent tasks can now run
     */
    private void checkDependentTasks(String completedTaskId) {
        for (PersistedTask task : taskCache.values()) {
            if (task.getState() == TaskState.WAITING_DEPENDENCY) {
                boolean allPrereqsComplete = true;
                for (String prereqId : task.getPrerequisiteTaskIds()) {
                    PersistedTask prereq = taskCache.get(prereqId);
                    if (prereq == null || prereq.getState() != TaskState.COMPLETED) {
                        allPrereqsComplete = false;
                        break;
                    }
                }
                if (allPrereqsComplete) {
                    updateTaskState(task.getTaskId(), TaskState.PENDING);
                }
            }
        }
    }
    
    /**
     * Save task result
     */
    public void saveTaskResult(String taskId, String result) {
        PersistedTask task = taskCache.get(taskId);
        if (task != null) {
            task.setResult(result);
            task.setState(TaskState.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(1.0);
            saveTask(task);
            
            // Notify dependent tasks
            checkDependentTasks(taskId);
        }
    }
    
    /**
     * Save task error
     */
    public void saveTaskError(String taskId, String error) {
        PersistedTask task = taskCache.get(taskId);
        if (task != null) {
            task.setErrorMessage(error);
            
            if (task.getCurrentRetries() < task.getMaxRetries()) {
                task.incrementRetries();
                task.setState(TaskState.PENDING); // Retry
                log.info("Task {} failed, retrying (attempt {}/{})", 
                        taskId, task.getCurrentRetries(), task.getMaxRetries());
            } else {
                task.setState(TaskState.FAILED);
                task.setCompletedAt(LocalDateTime.now());
                log.error("Task {} failed after {} retries: {}", 
                        taskId, task.getMaxRetries(), error);
            }
            
            saveTask(task);
        }
    }
    
    /**
     * Save task to disk
     */
    private void saveTask(PersistedTask task) {
        try {
            String filename = task.getTaskId() + ".json";
            Path path = tasksDirectory.resolve(filename);
            String content = serializeTask(task);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to save task {}: {}", task.getTaskId(), e.getMessage());
        }
    }
    
    /**
     * Serialize task to JSON
     */
    private String serializeTask(PersistedTask task) {
        // Simple JSON serialization (in production use Jackson/Gson)
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"taskId\": \"").append(task.getTaskId()).append("\",\n");
        sb.append("  \"parentTaskId\": \"").append(task.getParentTaskId()).append("\",\n");
        sb.append("  \"skillId\": \"").append(task.getSkillId()).append("\",\n");
        sb.append("  \"command\": \"").append(escapeJson(task.getCommand())).append("\",\n");
        sb.append("  \"state\": \"").append(task.getState()).append("\",\n");
        sb.append("  \"result\": \"").append(escapeJson(task.getResult())).append("\",\n");
        sb.append("  \"errorMessage\": \"").append(escapeJson(task.getErrorMessage())).append("\",\n");
        sb.append("  \"progress\": ").append(task.getProgress()).append(",\n");
        sb.append("  \"createdAt\": \"").append(task.getCreatedAt()).append("\",\n");
        sb.append("  \"startedAt\": \"").append(task.getStartedAt()).append("\",\n");
        sb.append("  \"completedAt\": \"").append(task.getCompletedAt()).append("\"\n");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Deserialize task from JSON
     */
    private PersistedTask deserializeTask(String content) {
        try {
            // Simple parsing (in production use Jackson/Gson)
            PersistedTask task = new PersistedTask();
            
            // Parse key fields - simplified
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("\"taskId\":")) {
                    task.setTaskId(extractValue(line));
                } else if (line.startsWith("\"skillId\":")) {
                    task.setSkillId(extractValue(line));
                } else if (line.startsWith("\"state\":")) {
                    String state = extractValue(line);
                    task.setState(TaskState.valueOf(state));
                }
            }
            
            return task;
        } catch (Exception e) {
            log.warn("Failed to parse task: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractValue(String line) {
        int colon = line.indexOf(':');
        if (colon > 0) {
            String value = line.substring(colon + 1).trim();
            if (value.startsWith("\"")) value = value.substring(1);
            if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
            if (value.endsWith("\"")) value = value.substring(0, value.length() - 1);
            return value;
        }
        return "";
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    /**
     * Delete task
     */
    public void deleteTask(String taskId) {
        taskCache.remove(taskId);
        try {
            Files.deleteIfExists(tasksDirectory.resolve(taskId + ".json"));
        } catch (IOException e) {
            log.warn("Failed to delete task file: {}", e.getMessage());
        }
    }
    
    /**
     * Get task statistics
     */
    public Map<String, Long> getTaskStatistics() {
        Map<String, Long> stats = new HashMap<>();
        for (TaskState state : TaskState.values()) {
            stats.put(state.name(), taskCache.values().stream()
                .filter(t -> t.getState() == state)
                .count());
        }
        return stats;
    }
}
