package com.craftpilot.llmservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CraftPilot LLM API")
                .description("CraftPilot LLM Servisi API Dokümantasyonu")
                .version("1.0"));
    }
}
