package com.DocSystem.agent.llm;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * LLM Service - integrates with Ollama for natural language responses
 */
@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    @Value("${llm.endpoint:http://localhost:11434}")
    private String endpoint;

    @Value("${llm.model:llama3}")
    private String defaultModel;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.max-tokens:2048}")
    private int maxTokens;

    @Value("${llm.connect-timeout:30}")
    private int connectTimeout;

    @Value("${llm.chat-timeout:60}")
    private int chatTimeout;

    @Value("${llm.embedding-timeout:10}")
    private int embeddingTimeout;

    @Value("${llm.api-key:}")
    private String apiKey;

    private OkHttpClient httpClient;

    /** Whether endpoint uses OpenAI-compatible format (vs Ollama native) */
    private boolean openAiCompatible = false;
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    @Value("${llm.backup.endpoint:}")
    private String backupEndpoint;

    @Value("${llm.backup.model:}")
    private String backupModel;

    private boolean hasBackup = false;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(chatTimeout, TimeUnit.SECONDS)
                .writeTimeout(chatTimeout, TimeUnit.SECONDS);

        // Try to use system proxy on Windows for external APIs (bigmodel.cn)
        try {
            ProxySelector defaultSelector = ProxySelector.getDefault();
            if (defaultSelector != null) {
                builder.proxySelector(defaultSelector);
                List<java.net.Proxy> proxies = defaultSelector.select(URI.create(endpoint));
                log.info("Proxy for {}: {}", endpoint, proxies);
            }
        } catch (Exception e) {
            log.warn("Could not set system proxy: {}", e.getMessage());
        }
        this.httpClient = builder.build();

        // Detect format: Ollama uses /api/chat with native format; OpenAI-compatible uses /v4/chat/completions
        this.openAiCompatible = endpoint != null && (
            endpoint.contains("openai.com") ||
            endpoint.contains("bigmodel.cn") ||
            endpoint.contains("deepseek.com") ||
            endpoint.contains("/v1/") ||
            endpoint.contains("/v4/"));
        this.hasBackup = backupEndpoint != null && !backupEndpoint.isEmpty();
        if (this.hasBackup) {
            log.info("LLM backup configured: endpoint={}, model={}", backupEndpoint,
                    backupModel.isEmpty() ? defaultModel : backupModel);
        }
        log.info("LLMService initialized: endpoint={}, model={}, connectTimeout={}s, chatTimeout={}s, openAiCompatible={}",
                endpoint, defaultModel, connectTimeout, chatTimeout, openAiCompatible);
    }

    /**
     * Check if primary LLM circuit breaker is open (per D-07).
     */
    private boolean isPrimaryCircuitOpen() {
        if (circuitBreakerRegistry == null) return false;
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("llm");
            return cb.getState() == CircuitBreaker.State.OPEN;
        } catch (Exception e) {
            log.debug("Could not check circuit breaker state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Execute LLM chat with a specific endpoint and model.
     * Extracted from chat() to support primary/backup switching.
     */
    private String doChat(String targetEndpoint, String targetModel,
                          List<Map<String, String>> messages) throws IOException {
        boolean targetOpenAi = targetEndpoint != null && (
            targetEndpoint.contains("openai.com") ||
            targetEndpoint.contains("bigmodel.cn") ||
            targetEndpoint.contains("deepseek.com") ||
            targetEndpoint.contains("/v1/") ||
            targetEndpoint.contains("/v4/"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", targetModel);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("stream", false);

        String json = JSON.toJSONString(requestBody);
        // Zhipu BigModel uses /v4; OpenAI, DeepSeek and most compatible APIs use /v1.
        String chatCompletionsPath = (targetEndpoint != null && targetEndpoint.contains("bigmodel.cn"))
                ? "/v4/chat/completions" : "/v1/chat/completions";
        String url = targetOpenAi ? targetEndpoint + chatCompletionsPath : targetEndpoint + "/api/chat";

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(JSON_MEDIA, json));
        if (apiKey != null && !apiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }
        Request request = reqBuilder.build();

        log.debug("LLM doChat: url={}, model={}, hasApiKey={}", url, targetModel, apiKey != null && !apiKey.isEmpty());
        Response response = httpClient.newCall(request).execute();
        String body = null;
        try {
            int code = response.code();
            ResponseBody respBody = response.body();
            body = respBody != null ? respBody.string() : "";
            log.info("LLM doChat response: status={}, bodyLen={}", code, body.length());

            if (code == 200) {
                JSONObject result = JSON.parseObject(body);
                String assistantMessage;

                if (targetOpenAi) {
                    JSONArray choices = result.getJSONArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                        assistantMessage = msg != null ? msg.getString("content") : "";
                    } else {
                        assistantMessage = "";
                    }
                } else {
                    JSONObject messageResult = result.getJSONObject("message");
                    assistantMessage = messageResult != null ? messageResult.getString("content") : "";
                }
                return assistantMessage;
            }

            log.warn("LLM doChat returned status {}: {}", code, body);
            throw new LlmHttpException(code, "LLM returned HTTP " + code + ": " + body);
        } finally {
            response.close();
        }
    }

    /**
     * Chat with LLM
     */
    public String chat(String message, String sessionId) throws IOException {
        return chat(message, sessionId, defaultModel);
    }

    /**
     * Chat with specific model
     */
    // Note: @CircuitBreaker and @Retry annotations removed for Spring 4 compatibility
    // Circuit breaker functionality available via isPrimaryCircuitOpen() check
    public String chat(String message, String sessionId, String model) throws IOException {
        // Get or create conversation history
        List<Map<String, String>> messages = conversationHistory.computeIfAbsent(
            sessionId, k -> new ArrayList<>()
        );

        // Add system prompt
        if (messages.isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are DocSys AI Assistant, a professional document management system assistant. " +
                       "You help users manage documents, repositories, and perform various tasks. " +
                       "You should be helpful, concise, and professional. " +
                       "When users ask about documents or repositories, you can help them manage their content.");
            messages.add(sysMsg);
        }

        // Add user message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", message);
        messages.add(userMsg);

        // Per D-06, D-07: Check circuit breaker state for primary/backup switching
        if (hasBackup && isPrimaryCircuitOpen()) {
            log.info("Primary LLM circuit breaker OPEN — switching to backup endpoint");
            String backupM = backupModel.isEmpty() ? defaultModel : backupModel;
            try {
                String response = doChat(backupEndpoint, backupM, messages);
                // Per D-08: Add to conversation history (preserved for next call)
                Map<String, String> asstMsg = new HashMap<>();
                asstMsg.put("role", "assistant");
                asstMsg.put("content", response);
                messages.add(asstMsg);
                while (messages.size() > 20) messages.remove(1);
                return response;
            } catch (Exception e) {
                log.error("Backup LLM also failed: {}", e.getMessage());
                throw e;  // Propagate to circuit breaker / fallback
            }
        }

        // Primary LLM call
        String response = doChat(endpoint, model, messages);
        // Add to conversation history
        Map<String, String> asstMsg2 = new HashMap<>();
        asstMsg2.put("role", "assistant");
        asstMsg2.put("content", response);
        messages.add(asstMsg2);
        while (messages.size() > 20) messages.remove(1);
        return response;
    }

    /**
     * Clear conversation history
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }

    /**
     * Get available models
     */
    public List<String> getAvailableModels() {
        try {
            String modelsPath = openAiCompatible ? "/v1/models" : "/api/tags";
            Request.Builder reqBuilder = new Request.Builder()
                    .url(endpoint + modelsPath)
                    .get();
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            Request request = reqBuilder.build();

            Response response = httpClient.newCall(request).execute();
            try {
                if (response.code() == 200) {
                    ResponseBody respBody = response.body();
                    String bodyStr = respBody != null ? respBody.string() : "";
                    if (openAiCompatible) {
                        // OpenAI/bigmodel format: {"data":[{"id":"model-name","object":"model"}]}
                        JSONObject result = JSON.parseObject(bodyStr);
                        JSONArray modelsArray = result.getJSONArray("data");
                        if (modelsArray != null) {
                            List<String> modelNames = new ArrayList<>();
                            for (int i = 0; i < modelsArray.size(); i++) {
                                JSONObject model = modelsArray.getJSONObject(i);
                                if (model != null) {
                                    modelNames.add(model.getString("id"));
                                }
                            }
                            return modelNames;
                        }
                    } else {
                        // Ollama format: {"models":[{"name":"..."}]}
                        JSONObject result = JSON.parseObject(bodyStr);
                        JSONArray modelsArray = result.getJSONArray("models");
                        if (modelsArray != null) {
                            List<String> modelNames = new ArrayList<>();
                            for (int i = 0; i < modelsArray.size(); i++) {
                                JSONObject model = modelsArray.getJSONObject(i);
                                if (model != null) {
                                    modelNames.add(model.getString("name"));
                                }
                            }
                            return modelNames;
                        }
                    }
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("Failed to get models", e);
        }

        return Arrays.asList(defaultModel);
    }

    /**
     * Check if LLM service is available
     */
    public boolean isAvailable() {
        try {
            // Probe the actual chat endpoint with a minimal request — this works for both
            // OpenAI-compatible (bigmodel.cn, deepseek, etc.) and Ollama.
            String chatPath = openAiCompatible
                    ? ((endpoint != null && endpoint.contains("bigmodel.cn")) ? "/v4/chat/completions" : "/v1/chat/completions")
                    : "/api/chat";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", defaultModel);
            Map<String, String> pingMsg = new HashMap<>();
            pingMsg.put("role", "user");
            pingMsg.put("content", "ping");
            requestBody.put("messages", Arrays.asList(pingMsg));
            requestBody.put("max_tokens", 1);
            requestBody.put("stream", false);
            String json = JSON.toJSONString(requestBody);

            Request.Builder reqBuilder = new Request.Builder()
                    .url(endpoint + chatPath)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(JSON_MEDIA, json));
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            Request request = reqBuilder.build();

            log.debug("LLM availability check: url={}", endpoint + chatPath);
            Response response = httpClient.newCall(request).execute();
            try {
                int code = response.code();
                log.info("LLM availability: status={}", code);
                // Accept any non-5xx status as "available" (4xx = auth/model issue, not connectivity)
                return code >= 200 && code < 500;
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.warn("LLM availability check failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Apply LLM configuration (called from SubAgent).
     */
    public void applyConfig(String url, String model, String apiKey) {
        if (url != null && !url.trim().isEmpty()) {
            this.endpoint = url;
        }
        if (model != null && !model.trim().isEmpty()) {
            this.defaultModel = model;
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            this.apiKey = apiKey;
        }
    }

    /**
     * Get configuration summary (called from SubAgent).
     */
    public String getConfigSummary() {
        return "endpoint=" + endpoint + ", model=" + defaultModel + ", apiKey=" + (apiKey != null ? "***" : "null");
    }

    /**
     * Error handler for LLM failures.
     * Returns a user-friendly message instead of a raw exception.
     */
    private String handleLLMError(String message, String sessionId, String model, Exception e) {
        log.error("LLM call failed: {}", e.getMessage());
        return "抱歉，AI 服务暂时不可用（" + e.getClass().getSimpleName() + "），请稍后重试。";
    }

    /**
     * Error handler for streaming chat failures.
     * Returns an iterator with a single error marker so the SSE handler emits a proper error.
     */
    private Iterator<String> handleStreamError(String message, String sessionId, String model, Exception e) {
        log.error("LLM streaming failed: {}", e.getMessage());
        return Arrays.asList("[ERROR] 抱歉，AI 服务暂时不可用（" + e.getClass().getSimpleName() + "），请稍后重试。").iterator();
    }

    /**
     * Error handler for streaming chat (3-param overload).
     */
    private Iterator<String> handleStreamError(String message, String sessionId, Exception e) {
        log.error("LLM streaming failed: {}", e.getMessage());
        return Arrays.asList("[ERROR] 抱歉，AI 服务暂时不可用（" + e.getClass().getSimpleName() + "），请稍后重试。").iterator();
    }

    /**
     * Streaming chat - returns lines for SSE
     * Each line is a JSON object with "content" field for partial responses
     * or "done" for completion
     */
    // Note: @CircuitBreaker and @Retry annotations removed for Spring 4 compatibility
    public Iterator<String> streamChat(String message, String sessionId) throws IOException {
        return streamChat(message, sessionId, defaultModel);
    }

    // Note: @CircuitBreaker and @Retry annotations removed for Spring 4 compatibility
    public Iterator<String> streamChat(String message, String sessionId, String model) throws IOException {
        List<String> chunks = new java.util.concurrent.CopyOnWriteArrayList<>();

        try {
            List<Map<String, String>> messages = conversationHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>()
            );

            if (messages.isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are DocSys AI Assistant. Keep responses concise and helpful.");
                messages.add(sysMsg);
            }
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            // Per D-07: Check circuit breaker state for primary/backup switching
            String streamEndpoint = endpoint;
            String streamModel = model;
            boolean streamOpenAi = openAiCompatible;
            if (hasBackup && isPrimaryCircuitOpen()) {
                log.info("Primary LLM circuit breaker OPEN for streaming — switching to backup");
                streamEndpoint = backupEndpoint;
                streamModel = backupModel.isEmpty() ? defaultModel : backupModel;
                // Detect format for backup endpoint
                streamOpenAi = streamEndpoint != null && (
                    streamEndpoint.contains("openai.com") ||
                    streamEndpoint.contains("bigmodel.cn") ||
                    streamEndpoint.contains("deepseek.com") ||
                    streamEndpoint.contains("/v1/") ||
                    streamEndpoint.contains("/v4/"));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", streamModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", true);

            String json = JSON.toJSONString(requestBody);
            String url = streamOpenAi ? streamEndpoint + "/v4/chat/completions" : streamEndpoint + "/api/chat";

            Request.Builder reqBuilder = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(JSON_MEDIA, json));
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            Request request = reqBuilder.build();

            Response response = httpClient.newCall(request).execute();
            try {
                if (response.code() == 200) {
                    ResponseBody respBody = response.body();
                    if (respBody != null) {
                        InputStream in = respBody.byteStream();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(in, StandardCharsets.UTF_8))) {
                            StringBuilder fullResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    line = line.substring(6);
                                }
                                if (line.isEmpty() || line.equals("[DONE]")) continue;

                                try {
                                    JSONObject obj = JSON.parseObject(line);
                                    String content = null;
                                    boolean done = false;

                                    if (streamOpenAi) {
                                        // OpenAI SSE format: {"choices":[{"delta":{"content":"..."}}]}
                                        JSONArray choices = obj.getJSONArray("choices");
                                        if (choices != null && !choices.isEmpty()) {
                                            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                            if (delta != null) content = delta.getString("content");
                                            // OpenAI uses choices[0].finish_reason != null to signal done
                                            String finishReason = choices.getJSONObject(0).getString("finish_reason");
                                            done = finishReason != null && !finishReason.isEmpty() && !"length".equals(finishReason);
                                        }
                                    } else {
                                        // Ollama SSE format: {"message":{"content":"..."},"done":true}
                                        JSONObject msg = obj.getJSONObject("message");
                                        if (msg != null) content = msg.getString("content");
                                        Boolean d = obj.getBoolean("done");
                                        done = d != null && d;
                                    }

                                    if (content != null && !content.isEmpty()) {
                                        fullResponse.append(content);
                                        chunks.add(content);
                                    }
                                    if (done) {
                                        chunks.add("[DONE]");
                                        // Add to history
                                        Map<String, String> asstMsg = new HashMap<>();
                                        asstMsg.put("role", "assistant");
                                        asstMsg.put("content", fullResponse.toString());
                                        messages.add(asstMsg);
                                        while (messages.size() > 20) messages.remove(1);
                                    }
                                } catch (Exception e) {
                                    // Skip malformed JSON
                                }
                            }
                        }
                    }
                } else {
                    // Non-200 response: throw so outer catch can rethrow to Resilience4j
                    throw new LlmHttpException(response.code(),
                        "LLM streaming returned HTTP " + response.code());
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("LLM streaming failed, propagating to circuit breaker", e);
            throw new RuntimeException("LLM streaming failed: " + e.getMessage(), e);
        }

        // Ensure [DONE] is always present
        if (!chunks.isEmpty() && !"[DONE]".equals(chunks.get(chunks.size() - 1))) {
            chunks.add("[DONE]");
        }
        return chunks.iterator();
    }
}
