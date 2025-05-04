package com.craftpilot.llmservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterConfig {
    private String apiKey;
    private String baseUrl;
    private String defaultModel;
    private Integer maxTokens;
    private Double temperature;
}