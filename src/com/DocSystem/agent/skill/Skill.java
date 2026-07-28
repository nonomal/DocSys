package com.DocSystem.agent.skill;

/**
 * Skill - represents a skill following agentskills.io standard
 */
public class Skill {
    
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final String permission;
    
    public Skill(String id, String name, String description) {
        this(id, name, description, "general", "read");
    }
    
    public Skill(String id, String name, String description, String category, String permission) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.permission = permission;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPermission() { return permission; }
    
    @Override
    public String toString() {
        return String.format("Skill[id=%s, name=%s, category=%s]", id, name, category);
    }
}
