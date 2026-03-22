package com.lingfeng.sprite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "sprite")
public class AppConfig {

    private Cognition cognition = new Cognition();
    private Llm llm = new Llm();

    public Cognition getCognition() {
        return cognition;
    }

    public void setCognition(Cognition cognition) {
        this.cognition = cognition;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public static class Cognition {
        private boolean enabled = true;
        private long intervalMs = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }
    }

    public static class Llm {
        private boolean enabled = true;
        private MinMax minmax = new MinMax();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public MinMax getMinmax() {
            return minmax;
        }

        public void setMinmax(MinMax minmax) {
            this.minmax = minmax;
        }
    }

    public static class MinMax {
        private String apiKey;
        private String baseUrl = "https://api.minimax.chat";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
