package com.craftpilot.llmservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterConfig {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String defaultModel = "google/gemini-2.0-flash-lite-001";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L;
}