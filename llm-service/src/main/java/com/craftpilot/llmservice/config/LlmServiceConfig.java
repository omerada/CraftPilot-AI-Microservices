package com.craftpilot.llmservice.config;

import com.craftpilot.llmservice.util.RequestBodyBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM Servisi için konfigürasyon sınıfı
 */
@Slf4j
@Configuration
public class LlmServiceConfig {

    /**
     * RequestBodyBuilder bean'ini açıkça tanımlar
     */
    @Bean
    public RequestBodyBuilder requestBodyBuilder(OpenRouterProperties openRouterProperties) {
        log.info("RequestBodyBuilder bean'i oluşturuluyor");
        return new RequestBodyBuilder(openRouterProperties);
    }
}
