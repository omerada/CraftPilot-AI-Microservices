package com.craftpilot.contentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAIConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer maxTokens;
    private Double temperature;

    @Bean
    public WebClient openaiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getModel() {
        return model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }
} 