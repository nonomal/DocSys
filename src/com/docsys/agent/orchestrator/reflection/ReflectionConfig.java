package com.docsys.agent.orchestrator.reflection;

import org.springframework.stereotype.Component;

/**
 * Reflection config — Spring 4 + plain @Component. Removed the
 * Spring-Boot-only @ConfigurationProperties since the gnhf/docsysagent-314649
 * base runs on Spring 4.3.8. Values are populated manually in
 * applicationContext.xml (or via system properties).
 */
@Component("reflectionConfig")
public class ReflectionConfig {

    private boolean enabled = false;
    private int maxAttempts = 2;
    private double llmTemperature = 0.2;
    private boolean enableMetrics = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("maxAttempts must be 1..10");
        }
        this.maxAttempts = maxAttempts;
    }

    public double getLlmTemperature() { return llmTemperature; }
    public void setLlmTemperature(double llmTemperature) {
        if (llmTemperature < 0.0 || llmTemperature > 1.5) {
            throw new IllegalArgumentException("llmTemperature must be 0.0..1.5");
        }
        this.llmTemperature = llmTemperature;
    }

    public boolean isEnableMetrics() { return enableMetrics; }
    public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
}
