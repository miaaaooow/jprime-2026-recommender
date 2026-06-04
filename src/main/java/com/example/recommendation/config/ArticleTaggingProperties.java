package com.example.recommendation.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.tagging")
public class ArticleTaggingProperties {

    private String extractor = "keyword";
    private final Smart smart = new Smart();

    public String getExtractor() {
        return extractor;
    }

    public void setExtractor(String extractor) {
        this.extractor = extractor;
    }

    public Smart getSmart() {
        return smart;
    }

    public static class Smart {

        private boolean enabled = false;
        private String provider = "openai";
        private String model = "gpt-5.4-nano";
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1/responses";
        private Duration timeout = Duration.ofSeconds(10);
        private int maxTopics = 5;
        private int maxTags = 8;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

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

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxTopics() {
            return maxTopics;
        }

        public void setMaxTopics(int maxTopics) {
            this.maxTopics = maxTopics;
        }

        public int getMaxTags() {
            return maxTags;
        }

        public void setMaxTags(int maxTags) {
            this.maxTags = maxTags;
        }
    }
}
