package com.docsys.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads and writes widget API key to .env file at app root.
 * Supports runtime hot-reload: Interceptor calls loadWidgetKey() on each request,
 * so changes take effect immediately without restart.
 *
 * .env format:
 *   DOCSYS_AGENT_WIDGET_KEY=dsa_xxxxx
 */
@Component
public class EnvConfig {

    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);

    private static final String KEY_NAME = "DOCSYS_AGENT_WIDGET_KEY";
    private static final String AGENT_URL_NAME = "DOCSYS_AGENT_URL";
    private static final String DOCSYS_URL_NAME = "DOCSYS_URL";
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^\\s*" + AGENT_URL_NAME + "\\s*=\\s*(.*)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCSYS_URL_PATTERN = Pattern.compile(
        "^\\s*" + DOCSYS_URL_NAME + "\\s*=\\s*(.*)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_PATTERN = Pattern.compile(
        "^\\s*" + KEY_NAME + "\\s*=\\s*(.*)\\s*$", Pattern.CASE_INSENSITIVE);

    private static volatile String cachedKey = null;
    private static volatile long lastRefreshMs = 0;
    private static volatile String cachedAgentUrl = null;
    private static volatile long lastAgentUrlRefreshMs = 0;
    private static final long REFRESH_INTERVAL_MS = 5_000; // refresh at most every 5s

    // LLM config keys in .env (supports multiple keys via index)
    private static final String LLM_KEY_PREFIX = "LLM_KEY_";
    private static volatile String cachedLlmConfig = null;
    private static volatile long lastLlmRefreshMs = 0;
    private static final long LLM_REFRESH_INTERVAL_MS = 5_000;

    /**
     * LLM config entry stored in .env.
     */
    public static class LlmConfig {
        private final String name;
        private final String endpoint;
        private final String model;
        private final String apiKey;

        public LlmConfig(String name, String endpoint, String model, String apiKey) {
            this.name = name;
            this.endpoint = endpoint;
            this.model = model;
            this.apiKey = apiKey;
        }

        public String name() { return name; }
        public String endpoint() { return endpoint; }
        public String model() { return model; }
        public String apiKey() { return apiKey; }
    }

    /**
     * Returns the Agent URL from .env, or empty if not set.
     * Used by AgentController /widget-config endpoint for adaptive widget deployment.
     * Caches for REFRESH_INTERVAL_MS.
     */
    public Optional<String> loadAgentUrl() {
        long now = System.currentTimeMillis();
        if (cachedAgentUrl != null && (now - lastAgentUrlRefreshMs) < REFRESH_INTERVAL_MS) {
            return Optional.ofNullable(cachedAgentUrl).filter(u -> u != null && !u.trim().isEmpty());
        }
        String envFile = findEnvFile();
        if (envFile == null) return Optional.empty();
        try {
            String url = readValueFromFile(Paths.get(envFile), URL_PATTERN);
            cachedAgentUrl = url;
            lastAgentUrlRefreshMs = now;
            return Optional.ofNullable(url).filter(u -> u != null && !u.trim().isEmpty());
        } catch (Exception e) {
            log.warn("Failed to read agent URL from .env: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the DocSystem URL from .env, or empty if not set.
     * Used by AgentController /widget-config endpoint.
     */
    public Optional<String> loadDocSysUrl() {
        String envFile = findEnvFile();
        if (envFile == null) return Optional.empty();
        try {
            return Optional.ofNullable(readValueFromFile(Paths.get(envFile), DOCSYS_URL_PATTERN))
                .filter(u -> u != null && !u.trim().isEmpty());
        } catch (Exception e) {
            log.warn("Failed to read DocSys URL from .env: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the widget API key from .env, or empty if not set.
     * Caches for REFRESH_INTERVAL_MS to avoid excessive file reads.
     */
    public Optional<String> loadWidgetKey() {
        long now = System.currentTimeMillis();
        if (cachedKey != null && (now - lastRefreshMs) < REFRESH_INTERVAL_MS) {
            return Optional.ofNullable(cachedKey).filter(k -> k != null && !k.trim().isEmpty());
        }

        String envFile = findEnvFile();
        if (envFile == null) {
            log.debug("No .env file found, widget key not configured");
            return Optional.empty();
        }

        try {
            String key = readKeyFromFile(Paths.get(envFile));
            cachedKey = key;
            lastRefreshMs = System.currentTimeMillis();
            return Optional.ofNullable(key).filter(k -> k != null && !k.trim().isEmpty());
        } catch (Exception e) {
            log.warn("Failed to read widget key from .env: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Writes the widget API key to .env file.
     * Creates the file if it doesn't exist.
     */
    public void saveWidgetKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }

        String envFile = findEnvFile();
        if (envFile == null) {
            // Create .env in app root
            envFile = Paths.get("").toAbsolutePath().resolve(".env").toString();
            log.info("Will create new .env file at: {}", envFile);
        }

        Path path = Paths.get(envFile);
        try {
            String content;
            if (Files.exists(path)) {
                content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                // Replace existing KEY= line or append
                String[] lines = content.split("\n", -1);
                boolean replaced = false;
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    if (KEY_PATTERN.matcher(line).matches()) {
                        sb.append(KEY_NAME).append("=").append(key).append("\n");
                        replaced = true;
                    } else {
                        sb.append(line).append("\n");
                    }
                }
                if (!replaced) {
                    sb.append(KEY_NAME).append("=").append(key).append("\n");
                }
                content = sb.toString();
            } else {
                content = KEY_NAME + "=" + key + "\n";
            }

            Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Invalidate cache so next read gets the new value
            cachedKey = key;
            lastRefreshMs = System.currentTimeMillis();

            log.info("Widget API key saved to {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write widget key to .env: {}", e.getMessage());
            throw new UncheckedIOException("Cannot write .env", e);
        }
    }

    /**
     * Returns the absolute path of the .env file, or null if not found and not creatable.
     */
    public String getEnvFilePath() {
        String found = findEnvFile();
        if (found != null) return found;
        return Paths.get("").toAbsolutePath().resolve(".env").toString();
    }

    // ── LLM Config (multiple keys) ──────────────────────────────────────────────

    /**
     * Load all LLM configs from .env.
     * Keys: LLM_KEY_<name> = <endpoint> | <model> | <apiKey>
     */
    public List<LlmConfig> loadLlmConfigs() {
        long now = System.currentTimeMillis();
        if (cachedLlmConfig != null && (now - lastLlmRefreshMs) < LLM_REFRESH_INTERVAL_MS) {
            return parseLlmConfigs(cachedLlmConfig);
        }

        String envFile = findEnvFile();
        if (envFile == null) {
            return new java.util.ArrayList<>();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(envFile)), StandardCharsets.UTF_8);
            cachedLlmConfig = content;
            lastLlmRefreshMs = System.currentTimeMillis();
            return parseLlmConfigs(content);
        } catch (IOException e) {
            log.warn("Failed to load LLM configs from .env: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    private List<LlmConfig> parseLlmConfigs(String content) {
        List<LlmConfig> result = new java.util.ArrayList<>();
        Pattern p = Pattern.compile("^\\s*LLM_KEY_([^=]+)\\s*=\\s*(.*)\\s*$",
            Pattern.CASE_INSENSITIVE);
        for (String line : content.split("\n", -1)) {
            java.util.regex.Matcher m = p.matcher(line);
            if (m.matches()) {
                String name = m.group(1).trim();
                String value = m.group(2).trim();
                String[] parts = value.split("\\s*\\|\\s*", 4);
                if (parts.length >= 3) {
                    result.add(new LlmConfig(
                        name,
                        parts[0].trim(),
                        parts[1].trim(),
                        parts.length > 3 ? parts[3].trim() : ""
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Save an LLM config to .env, replacing existing entry with the same name.
     * Format: LLM_KEY_<name> = <endpoint> | <model> | <apiKey>
     */
    public void saveLlmConfig(String name, String endpoint, String model, String apiKey) {
        String entryKey = LLM_KEY_PREFIX + name;
        String entryValue = endpoint + " | " + model + " | " + apiKey;

        String envFile = getEnvFilePath();
        Path path = Paths.get(envFile);

        try {
            String content = Files.exists(path)
                ? new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                : "";

            String[] lines = content.split("\n", -1);
            Pattern pat = Pattern.compile("^\\s*" + Pattern.quote(entryKey) + "\\s*=", Pattern.CASE_INSENSITIVE);
            boolean replaced = false;
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (!replaced && pat.matcher(line).find()) {
                    sb.append(entryKey).append(" = ").append(entryValue).append("\n");
                    replaced = true;
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (!replaced) {
                sb.append(entryKey).append(" = ").append(entryValue).append("\n");
            }

            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Invalidate cache
            cachedLlmConfig = null;
            lastLlmRefreshMs = 0;

            log.info("LLM config saved: name={}, endpoint={}, model={}", name, endpoint, model);
        } catch (IOException e) {
            log.error("Failed to save LLM config: {}", e.getMessage());
            throw new UncheckedIOException("Cannot write .env", e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String findEnvFile() {
        // Search in app root and parent dirs
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 3; i++) {
            Path env = dir.resolve(".env");
            if (Files.exists(env)) {
                return env.toString();
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return null;
    }

    private String readKeyFromFile(Path path) throws IOException {
        return readValueFromFile(path, KEY_PATTERN);
    }

    private String readValueFromFile(Path path, Pattern pattern) throws IOException {
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            java.util.regex.Matcher m = pattern.matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }
}
