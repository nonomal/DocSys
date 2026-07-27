package com.docsys.agent.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SkillCommand - CLI command template
 */
public class SkillCommand {
    
    private String pattern;
    private String cliCommand;
    private String description;
    private Map<String, SkillParameter> parameters;
    private Pattern compiledPattern;
    
    public SkillCommand() {
        this.parameters = new HashMap<>();
    }
    
    /**
     * Check if query matches this command pattern
     */
    public boolean matches(String query) {
        if (query == null || pattern == null) return false;
        
        // Simple contains matching
        if (query.toLowerCase().contains(pattern.toLowerCase())) {
            return true;
        }
        
        // Check if CLI command is in query
        if (cliCommand != null && query.toLowerCase().contains(cliCommand.toLowerCase())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract parameters from query
     */
    public Map<String, String> extractParameters(String query) {
        Map<String, String> result = new HashMap<>();
        
        // Simple extraction - split by spaces
        String[] parts = query.trim().split("\\s+");
        
        int paramIndex = 0;
        for (String part : parts) {
            if (part.startsWith("<") && part.endsWith(">")) {
                String paramName = part.substring(1, part.length() - 1);
                // Look for next part as value
                // This is simplified - real implementation would use NLP
            }
        }
        
        return result;
    }
    
    // Getters and Setters
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { 
        this.pattern = pattern;
        // Compile pattern for faster matching
        if (pattern != null) {
            // Simple wildcard pattern
            String regex = pattern
                .replace("<", "(?<")
                .replace(">", ">[^\\s]+)");
            this.compiledPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }
    
    public String getCliCommand() { return cliCommand; }
    public void setCliCommand(String cliCommand) { this.cliCommand = cliCommand; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, SkillParameter> getParameters() { return parameters; }
    public void addParameter(String name, String description, boolean required) {
        this.parameters.put(name, new SkillParameter(name, description, required));
    }
}
