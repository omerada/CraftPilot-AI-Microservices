package com.craftpilot.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                    new Server().url("/"),
                    new Server().url("http://api.craftpilot.io"),
                    new Server().url("https://api.craftpilot.io")
                ))
                .info(new Info()
                    .title(applicationName.toUpperCase() + " API Documentation")
                    .version("1.0")
                    .description("API Gateway Documentation for CraftPilot AI Platform"));
    }
}
