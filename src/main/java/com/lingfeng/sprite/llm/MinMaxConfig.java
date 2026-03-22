package com.lingfeng.sprite.llm;

/**
 * MinMax LLM 配置
 */
public record MinMaxConfig(
    String apiKey,
    String baseUrl,
    String model,
    long timeoutMs
) {
    public MinMaxConfig {
        if (baseUrl == null) baseUrl = "https://api.minimax.chat/v1";
        if (model == null) model = "MiniMax-Text-01";
        if (timeoutMs == 0) timeoutMs = 30000;
    }

    public MinMaxConfig(String apiKey) {
        this(apiKey, "https://api.minimax.chat/v1", "MiniMax-Text-01", 30000);
    }

    public MinMaxConfig(String apiKey, String baseUrl, String model) {
        this(apiKey, baseUrl, model, 30000);
    }
}
