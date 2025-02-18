package com.craftpilot.llmservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springdoc.core.GroupedOpenApi;
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
                .version("1.0")
                .contact(new Contact()
                    .name("CraftPilot")
                    .url("https://craftpilot.io")))
            .externalDocs(new ExternalDocumentation()
                .description("API Dokümantasyonu")
                .url("https://craftpilot.io/docs"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/ai/**")
            .build();
    }
}
