package com.DocSystem.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * SkillMemoryManager - Progressive Disclosure Memory System
 * 
 * Key Design Principles:
 * 1. Skill-like organization - memories are skills that can be loaded/unloaded
 * 2. Progressive disclosure - only show relevant context, not everything
 * 3. Context compression - compress old context to save tokens
 * 4. Auto-organization - organize memories for easy retrieval
 * 
 * Memory Layers (from recent to old):
 * - Working Memory: Current session context (in-memory)
 * - Session Memory: Recent sessions (compressed)
 * - Long-term Memory: Organized by skill/topic
 * - Archive: Old memories (rarely accessed)
 */
@Deprecated
public class SkillMemoryManager {
    
    private static final Logger log = LoggerFactory.getLogger(SkillMemoryManager.class);
    private static SkillMemoryManager instance;
    
    // Configuration
    private static final int MAX_WORKING_MEMORY = 50;  // Max messages in working memory
    private static final int COMPRESSION_THRESHOLD = 30; // When to compress
    private static final int CONTEXT_WINDOW_LIMIT = 4000; // Approximate token limit
    
    // Storage paths
    private final Path memoryDirectory;
    private final Path workingMemoryFile;
    private final Path sessionMemoryDir;
    private final Path longTermMemoryDir;
    private final Path archiveDir;
    
    // In-memory structures
    private final ConcurrentLinkedQueue<MemoryEntry> workingMemory;
    private final Map<String, MemorySkill> memorySkills; // Skill-like memory units
    
    // Compression engine
    private final MemoryCompression compression;
    
    private SkillMemoryManager() {
        // Initialize directories
        String userDir = System.getProperty("user.dir", ".");
        this.memoryDirectory = Paths.get(userDir, "data", "memory");
        
        this.workingMemoryFile = memoryDirectory.resolve("working_memory.json");
        this.sessionMemoryDir = memoryDirectory.resolve("sessions");
        this.longTermMemoryDir = memoryDirectory.resolve("longterm");
        this.archiveDir = memoryDirectory.resolve("archive");
        
        try {
            Files.createDirectories(memoryDirectory);
            Files.createDirectories(sessionMemoryDir);
            Files.createDirectories(longTermMemoryDir);
            Files.createDirectories(archiveDir);
        } catch (IOException e) {
            log.error("Failed to create memory directories", e);
        }
        
        this.workingMemory = new ConcurrentLinkedQueue<>();
        this.memorySkills = new ConcurrentHashMap<>();
        this.compression = new MemoryCompression();
        
        // Load existing memories
        loadWorkingMemory();
        loadMemorySkills();
    }
    
    public static synchronized SkillMemoryManager getInstance() {
        if (instance == null) {
            instance = new SkillMemoryManager();
        }
        return instance;
    }
    
    // ========== Memory Entry ==========
    
    public static class MemoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String id;
        private String role; // "user", "assistant", "system", "tool"
        private String content;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
        private String skillId; // Associated skill
        private double importance; // 0.0 - 1.0
        
        public MemoryEntry(String role, String content) {
            this.id = UUID.randomUUID().toString();
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.metadata = new HashMap<>();
            this.importance = calculateImportance(content);
        }
        
        private double calculateImportance(String content) {
            // Simple heuristic - important content gets higher scores
            if (content.contains("error") || content.contains("failed")) return 0.8;
            if (content.contains("success") || content.contains("completed")) return 0.6;
            if (content.contains("?")) return 0.5;
            return 0.3;
        }
        
        // Getters
        public String getId() { return id; }
        public String getRole() { return role; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        public double getImportance() { return importance; }
        public void setImportance(double importance) { this.importance = importance; }
    }
    
    // ========== Memory Skill (Skill-like memory unit) ==========
    
    /**
     * MemorySkill - A skill-like memory unit
     * Instead of hardcoded memory, memories are organized as skills
     * that can be loaded when needed
     */
    public static class MemorySkill implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String id;
        private String name;
        private String category; // "user_preference", "task_pattern", "context", "learned"
        private List<String> keywords;
        private String summary; // Compressed summary
        private List<MemoryEntry> entries;
        private LocalDateTime lastAccessed;
        private int accessCount;
        private Map<String, Object> metadata;
        
        public MemorySkill(String id, String name, String category) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.keywords = new ArrayList<>();
            this.entries = new ArrayList<>();
            this.lastAccessed = LocalDateTime.now();
            this.accessCount = 0;
            this.metadata = new HashMap<>();
        }
        
        /**
         * Add entry to this memory skill
         */
        public void addEntry(MemoryEntry entry) {
            entries.add(entry);
            updateSummary();
        }
        
        /**
         * Update summary by compressing entries
         */
        private void updateSummary() {
            if (entries.isEmpty()) {
                summary = "";
                return;
            }
            
            // Keep only important entries
            List<MemoryEntry> important = entries.stream()
                .filter(e -> e.getImportance() > 0.5)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .collect(Collectors.toList());
            
            // Build summary
            StringBuilder sb = new StringBuilder();
            sb.append("## ").append(name).append("\n");
            sb.append("Last updated: ").append(lastAccessed).append("\n");
            sb.append("Access count: ").append(accessCount).append("\n\n");
            
            for (MemoryEntry e : important) {
                sb.append("- [").append(e.getRole()).append("] ")
                  .append(truncate(e.getContent(), 100)).append("\n");
            }
            
            summary = sb.toString();
        }
        
        private String truncate(String s, int len) {
            if (s == null) return "";
            return s.length() > len ? s.substring(0, len) + "..." : s;
        }
        
        public void markAccessed() {
            lastAccessed = LocalDateTime.now();
            accessCount++;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public List<String> getKeywords() { return keywords; }
        public String getSummary() { return summary; }
        public List<MemoryEntry> getEntries() { return entries; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public int getAccessCount() { return accessCount; }
    }
    
    // ========== Core Operations ==========
    
    /**
     * Add memory entry to working memory
     */
    public void addEntry(String role, String content, String skillId) {
        MemoryEntry entry = new MemoryEntry(role, content);
        entry.setSkillId(skillId);
        
        workingMemory.add(entry);
        
        // Add to appropriate memory skill
        if (skillId != null) {
            addToMemorySkill(skillId, entry);
        }
        
        // Check if compression needed
        if (workingMemory.size() > COMPRESSION_THRESHOLD) {
            compressWorkingMemory();
        }
        
        // Persist
        saveWorkingMemory();
    }
    
    /**
     * Add to memory skill
     */
    private void addToMemorySkill(String skillId, MemoryEntry entry) {
        MemorySkill skill = memorySkills.get(skillId);
        if (skill == null) {
            skill = new MemorySkill(skillId, skillId, "context");
            memorySkills.put(skillId, skill);
        }
        skill.addEntry(entry);
        saveMemorySkill(skill);
    }
    
    /**
     * Get context for LLM - progressive disclosure
     * Returns only the most relevant memories based on current query
     */
    public List<MemoryEntry> getContextForQuery(String query) {
        List<MemoryEntry> context = new ArrayList<>();
        
        // 1. Get recent working memory (most recent first)
        List<MemoryEntry> recent = workingMemory.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(10)
            .collect(Collectors.toList());
        context.addAll(recent);
        
        // 2. Find relevant memory skills
        if (query != null && !query.isEmpty()) {
            List<MemorySkill> relevantSkills = findRelevantSkills(query);
            
            // Add summaries from relevant skills (progressive disclosure)
            for (MemorySkill skill : relevantSkills.stream().limit(3).collect(Collectors.toList())) {
                skill.markAccessed();
                
                // Add skill summary as context
                if (skill.getSummary() != null) {
                    MemoryEntry summaryEntry = new MemoryEntry("system", 
                        "## Memory: " + skill.getName() + "\n" + skill.getSummary());
                    summaryEntry.setSkillId(skill.getId());
                    context.add(summaryEntry);
                }
            }
        }
        
        // 3. Check context limit
        while (estimateTokenCount(context) > CONTEXT_WINDOW_LIMIT) {
            // Remove oldest entries
            if (!context.isEmpty()) {
                context.remove(context.size() - 1);
            } else {
                break;
            }
        }
        
        return context;
    }
    
    /**
     * Find relevant memory skills for query
     */
    private List<MemorySkill> findRelevantSkills(String query) {
        if (query == null) return new ArrayList<>();
        
        final String lowerQuery = query.toLowerCase();
        
        // Helper to check if skill matches query
        List<MemorySkill> matching = new ArrayList<>();
        for (MemorySkill skill : memorySkills.values()) {
            if (matchesSkill(skill, lowerQuery)) {
                matching.add(skill);
            }
        }
        
        // Sort by recency and access count
        matching.sort((a, b) -> {
            long scoreA = a.getAccessCount() * 30 + 
                Duration.between(a.getLastAccessed(), LocalDateTime.now()).toMinutes();
            long scoreB = b.getAccessCount() * 30 + 
                Duration.between(b.getLastAccessed(), LocalDateTime.now()).toMinutes();
            return Long.compare(scoreA, scoreB);
        });
        
        return matching;
    }
    
    private boolean matchesSkill(MemorySkill skill, String query) {
        // Check keywords
        for (String kw : skill.getKeywords()) {
            if (query.contains(kw.toLowerCase())) {
                return true;
            }
        }
        // Check category
        String cat = skill.getCategory();
        if (cat != null && query.contains(cat.toLowerCase())) {
            return true;
        }
        return false;
    }
    
    // ========== Context Compression ==========
    
    /**
     * Compress working memory when threshold reached
     */
    private void compressWorkingMemory() {
        log.info("Compressing working memory (size: {})", workingMemory.size());
        
        // 1. Separate important and less important entries
        List<MemoryEntry> important = workingMemory.stream()
            .filter(e -> e.getImportance() > 0.4)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        List<MemoryEntry> compressible = workingMemory.stream()
            .filter(e -> e.getImportance() <= 0.4)
            .collect(Collectors.toList());
        
        // 2. Compress less important entries
        List<MemoryEntry> compressed = compression.compress(compressible);
        
        // 3. Merge and keep in working memory
        workingMemory.clear();
        important.addAll(compressed);
        
        // Keep only MAX_WORKING_MEMORY
        workingMemory.addAll(important.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(MAX_WORKING_MEMORY)
            .collect(Collectors.toList()));
        
        // 4. Archive compressed to long-term memory
        if (!compressed.isEmpty()) {
            archiveToLongTerm(compressed);
        }
        
        log.info("Compressed to {} entries", workingMemory.size());
    }
    
    /**
     * Archive to long-term memory
     */
    private void archiveToLongTerm(List<MemoryEntry> entries) {
        String archiveName = "archive_" + LocalDateTime.now().toString().replace(":", "-");
        
        try {
            Path archiveFile = archiveDir.resolve(archiveName + ".json");
            String content = serializeEntries(entries);
            Files.write(archiveFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to archive memory", e);
        }
    }
    
    // ========== Memory Compression ==========
    
    /**
     * MemoryCompression - compresses old context
     */
    static class MemoryCompression {
        
        /**
         * Compress a list of memory entries
         */
        List<MemoryEntry> compress(List<MemoryEntry> entries) {
            if (entries == null || entries.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Group by conversation/skill
            Map<String, List<MemoryEntry>> groups = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getSkillId() != null ? e.getSkillId() : "default"));
            
            List<MemoryEntry> compressed = new ArrayList<>();
            
            for (Map.Entry<String, List<MemoryEntry>> group : groups.entrySet()) {
                List<MemoryEntry> groupEntries = group.getValue();
                
                if (groupEntries.size() <= 3) {
                    // Keep small groups as-is
                    compressed.addAll(groupEntries);
                } else {
                    // Summarize large groups
                    MemoryEntry summary = summarizeEntries(group.getKey(), groupEntries);
                    compressed.add(summary);
                }
            }
            
            return compressed;
        }
        
        /**
         * Summarize multiple entries into one
         */
        private MemoryEntry summarizeEntries(String skillId, List<MemoryEntry> entries) {
            // Build summary from first and last entries
            StringBuilder summary = new StringBuilder();
            summary.append("[Compressed ").append(entries.size()).append(" messages] ");
            
            // First message
            if (!entries.isEmpty()) {
                summary.append("First: ").append(truncate(entries.get(0).getContent(), 50));
            }
            
            // Last message
            if (entries.size() > 1) {
                summary.append(" | Last: ").append(truncate(entries.get(entries.size()-1).getContent(), 50));
            }
            
            // Count by role
            long userCount = entries.stream().filter(e -> "user".equals(e.getRole())).count();
            long assistantCount = entries.stream().filter(e -> "assistant".equals(e.getRole())).count();
            summary.append(" | User: ").append(userCount).append(" | Assistant: ").append(assistantCount);
            
            MemoryEntry entry = new MemoryEntry("system", summary.toString());
            entry.setSkillId(skillId);
            entry.setImportance(0.5);
            
            return entry;
        }
        
        private String truncate(String s, int len) {
            if (s == null) return "";
            return s.length() > len ? s.substring(0, len) + "..." : s;
        }
    }
    
    // ========== Persistence ==========
    
    private void saveWorkingMemory() {
        try {
            List<MemoryEntry> list = new ArrayList<>(workingMemory);
            String content = serializeEntries(list);
            Files.write(workingMemoryFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to save working memory", e);
        }
    }
    
    private void loadWorkingMemory() {
        try {
            if (Files.exists(workingMemoryFile)) {
                String content = new String(Files.readAllBytes(workingMemoryFile));
                List<MemoryEntry> entries = deserializeEntries(content);
                workingMemory.addAll(entries);
                log.info("Loaded {} working memory entries", entries.size());
            }
        } catch (Exception e) {
            log.error("Failed to load working memory", e);
        }
    }
    
    private void saveMemorySkill(MemorySkill skill) {
        try {
            Path file = longTermMemoryDir.resolve(skill.getId() + ".json");
            // Simplified serialization
            String content = "skill:" + skill.getId() + ",name:" + skill.getName();
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to save memory skill", e);
        }
    }
    
    private void loadMemorySkills() {
        try {
            Files.list(longTermMemoryDir)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    log.debug("Found memory skill: {}", p.getFileName());
                });
        } catch (IOException e) {
            log.error("Failed to load memory skills", e);
        }
    }
    
    private String serializeEntries(List<MemoryEntry> entries) {
        // Simplified JSON serialization
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < entries.size(); i++) {
            MemoryEntry e = entries.get(i);
            sb.append("{\"id\":\"").append(e.getId())
              .append("\",\"role\":\"").append(e.getRole())
              .append("\",\"content\":\"").append(e.getContent().replace("\"", "\\\""))
              .append("\",\"timestamp\":\"").append(e.getTimestamp())
              .append("\",\"importance\":").append(e.getImportance()).append("}");
            if (i < entries.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private List<MemoryEntry> deserializeEntries(String content) {
        List<MemoryEntry> entries = new ArrayList<>();
        // Simplified parsing - in production use Jackson
        return entries;
    }
    
    // ========== Utilities ==========
    
    private int estimateTokenCount(List<MemoryEntry> entries) {
        // Rough estimate: 1 token ≈ 4 characters
        int chars = entries.stream()
            .mapToInt(e -> e.getContent().length())
            .sum();
        return chars / 4;
    }
    
    /**
     * Get memory statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("workingMemorySize", workingMemory.size());
        stats.put("memorySkillsCount", memorySkills.size());
        
        try {
            stats.put("longTermMemoryFiles", 
                Files.list(longTermMemoryDir).count());
            stats.put("archivedFiles", 
                Files.list(archiveDir).count());
        } catch (IOException e) {
            // Ignore
        }
        
        return stats;
    }
    
    /**
     * Clear all memories (for testing)
     */
    public void clear() {
        workingMemory.clear();
        memorySkills.clear();
        saveWorkingMemory();
    }
}
