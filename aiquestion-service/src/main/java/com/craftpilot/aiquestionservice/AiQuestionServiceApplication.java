package com.craftpilot.aiquestionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableDiscoveryClient
@EnableWebFlux
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "AI Question Service API",
        version = "1.0",
        description = "REST API for AI Question Service"
    )
)
public class AiQuestionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiQuestionServiceApplication.class, args);
    }
} 