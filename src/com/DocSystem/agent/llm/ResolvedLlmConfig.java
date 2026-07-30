package com.DocSystem.agent.llm;

/**
 * 已解析的、请求级别的 LLM 配置 —— 不可变值对象。
 *
 * <p>用于把"用户选定的模型"从 Controller 贯穿到 {@link LLMService} 的无状态入口，
 * 全程作为方法参数/局部变量传递，绝不写入 LLMService 的共享可变字段，
 * 因此多个并发 SSE 请求各自选不同模型时互不干扰。
 */
public final class ResolvedLlmConfig {

    public final String endpoint;
    public final String model;
    public final String apiKey;
    public final boolean openAiCompatible;
    /** 显示名，仅用于日志/回显，不参与请求 */
    public final String displayName;

    public ResolvedLlmConfig(String endpoint, String model, String apiKey, String displayName) {
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
        this.displayName = displayName;
        this.openAiCompatible = LLMService.detectOpenAiCompatible(endpoint);
    }

    @Override
    public String toString() {
        return "ResolvedLlmConfig{name=" + displayName + ", endpoint=" + endpoint
                + ", model=" + model + ", openAiCompatible=" + openAiCompatible
                + ", apiKey=" + (apiKey != null && !apiKey.isEmpty() ? "***" : "null") + "}";
    }
}
