package com.craftpilot.llmservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "openrouter")
@Data
public class OpenRouterConfig {
    
    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url:https://api.openrouter.ai/api}")
    private String apiUrl;

    private String baseUrl = "https://openrouter.ai/api/v1";
    private String defaultModel = "google/gemini-2.0-flash-lite-preview-02-05:free";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L;

    @Bean
    public WebClient openRouterWebClient() {
        return WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("HTTP-Referer", "https://craftpilot.io")
            .defaultHeader("X-Title", "CraftPilot AI")
            .build();
    }
}