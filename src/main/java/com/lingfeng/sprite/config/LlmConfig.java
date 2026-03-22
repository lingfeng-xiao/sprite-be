package com.lingfeng.sprite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * LLM 配置
 */
@Configuration
public class LlmConfig {

    @Bean
    public MinMaxConfig minMaxConfig(AppConfig appConfig) {
        return new MinMaxConfig(
                appConfig.getLlm().getMinmax().getApiKey(),
                appConfig.getLlm().getMinmax().getBaseUrl(),
                "MiniMax-Text-01"
        );
    }

    @Bean
    public MinMaxLlmReasoner minMaxLlmReasoner(MinMaxConfig minMaxConfig) {
        return new MinMaxLlmReasoner(minMaxConfig);
    }
}
