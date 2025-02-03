package com.craftpilot.analyticsservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Analytics Service API")
                        .version("1.0")
                        .description("Analytics Service for Craft Pilot AI"))
                .servers(List.of(
                        new Server().url("http://152.53.115.63:" + serverPort)
                                .description("Production server"),
                        new Server().url("http://localhost:" + serverPort)
                                .description("Local server")
                ));
    }
} 