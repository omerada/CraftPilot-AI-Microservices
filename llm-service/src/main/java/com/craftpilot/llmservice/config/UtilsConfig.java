package com.craftpilot.llmservice.config;

import com.craftpilot.llmservice.util.RequestBodyBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Utility sınıfları için yapılandırma
 */
@Configuration
public class UtilsConfig {

    /**
     * RequestBodyBuilder bean'ini manuel olarak tanımlar
     * Bu, OpenRouterClient'ın RequestBodyBuilder'a autowire edilebilmesini sağlar
     */
    @Bean
    public RequestBodyBuilder requestBodyBuilder(OpenRouterProperties properties) {
        return new RequestBodyBuilder(properties);
    }
}
