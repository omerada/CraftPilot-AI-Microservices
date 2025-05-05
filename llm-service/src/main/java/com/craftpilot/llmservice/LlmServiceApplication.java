package com.craftpilot.llmservice;

import com.craftpilot.llmservice.config.LightSecurityConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = { "com.craftpilot.llmservice" })
@Import(LightSecurityConfig.class)
@OpenAPIDefinition(info = @Info(title = "LLM Service API", version = "1.0.0", description = "Service for AI-powered language model interactions"))
public class LlmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }
}
