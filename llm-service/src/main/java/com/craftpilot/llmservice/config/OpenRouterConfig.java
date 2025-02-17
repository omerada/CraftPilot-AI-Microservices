package com.craftpilot.llmservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openrouter")
@Data
public class OpenRouterConfig {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String defaultModel = "openai/gpt-3.5-turbo";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L;
} 