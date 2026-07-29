package com.DocSystem.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration placeholder for Spring 4.x.
 * Spring Data Redis is not available in Spring 4.x - requires Spring Boot.
 * Redis functionality should be accessed via MyBatis/MySQL for session persistence.
 * This config class remains for future Jedis integration if needed.
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Placeholder - Redis features disabled for Spring 4 compatibility.
     * For Redis support, consider using Jedis directly or implement a RedisService.
     */
    public RedisConfig() {
        log.info("RedisConfig initialized - Spring Data Redis not available in Spring 4.x");
    }
}