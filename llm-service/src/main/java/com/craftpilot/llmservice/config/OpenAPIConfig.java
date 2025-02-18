package com.craftpilot.llmservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server()
            .url("http://localhost:8062")
            .description("Local Development");
            
        Server prodServer = new Server()
            .url("https://api.craftpilot.io")
            .description("Production");

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
                .url("https://craftpilot.io/docs"))
            .servers(List.of(localServer, prodServer));
    }
}
