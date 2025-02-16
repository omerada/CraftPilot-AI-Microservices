package com.craftpilot.llmservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import com.craftpilot.llmservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@Import(LightSecurityConfig.class) 
@SpringBootApplication
@EnableDiscoveryClient
@EnableWebFlux
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "LLM Service API",
        version = "1.0",
        description = "REST API for LLM Service"
    )
)
public class LlmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }
} 