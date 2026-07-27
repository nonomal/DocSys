package com.docsys.agent.skill;

/**
 * SkillParameter - parameter definition
 */
public class SkillParameter {
    
    private String name;
    private String description;
    private boolean required;
    private String type;  // string, number, boolean, file, etc.
    private String defaultValue;
    private String[] allowedValues;
    
    public SkillParameter() {}
    
    public SkillParameter(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.type = "string";
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    
    public String[] getAllowedValues() { return allowedValues; }
    public void setAllowedValues(String[] allowedValues) { this.allowedValues = allowedValues; }
}
