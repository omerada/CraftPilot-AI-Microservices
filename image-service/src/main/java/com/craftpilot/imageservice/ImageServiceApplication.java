package com.craftpilot.imageservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.craftpilot.imageservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling 
@Import(LightSecurityConfig.class)
@OpenAPIDefinition(
    info = @Info(
        title = "Image Service API",
        version = "1.0.0",
        description = "Service for AI-powered image generation"
    )
)
public class ImageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }
} 