package com.craftpilot.llmservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String defaultModel = "google/gemini-2.0-flash-lite-001";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L;
    private Integer requestTimeoutSeconds = 60;
    private Integer streamTimeoutSeconds = 120;
    private Integer keepAliveIntervalSeconds = 15;
    private String defaultSystemPrompt = "Sen yardımcı bir yapay zeka asistanısın.";
}
