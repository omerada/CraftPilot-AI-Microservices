package com.craftpilot.llmservice.config;

import com.craftpilot.llmservice.util.RequestBodyBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Yardımcı bean'lerin açık yapılandırması
 */
@Configuration
public class UtilBeansConfig {

    /**
     * RequestBodyBuilder bean'ini açık olarak yapılandırır
     * Bu yöntem, component tarama sorunlarını aşar
     */
    @Bean
    @Primary
    public RequestBodyBuilder requestBodyBuilder(OpenRouterProperties properties) {
        return new RequestBodyBuilder(properties);
    }
}
