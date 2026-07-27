package com.docsys.agent.nlu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Skill intent registry — stub implementation.
 *
 * The original gnhf/docsysagent-314649 base referenced this class from
 * LLMIntentParser and MainAgent, but the source file was never committed.
 * This stub provides the minimum surface so the project compiles and
 * the existing controller flow continues to work. Real external-skill
 * injection (dynamic LLM prompt enrichment) is left as a follow-up.
 */
public class SkillIntentRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillIntentRegistry.class);

    private static volatile SkillIntentRegistry _instance;
    public static SkillIntentRegistry getInstance() {
        if (_instance == null) {
            synchronized (SkillIntentRegistry.class) {
                if (_instance == null) {
                    _instance = new SkillIntentRegistry();
                }
            }
        }
        return _instance;
    }

    /** Return an empty list (no external skills registered in this build). */
    public List<String> getRegisteredSkillIds() {
        return Collections.emptyList();
    }

    /** Return an empty prompt section so the LLM sees no extra skills. */
    public String buildSkillPromptSection() {
        return "";
    }

    /** Whether a given skillId is registered (always false in the stub). */
    public boolean isRegistered(String skillId) {
        return false;
    }
}
