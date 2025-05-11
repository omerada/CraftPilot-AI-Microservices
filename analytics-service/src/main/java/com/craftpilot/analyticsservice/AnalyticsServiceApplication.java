package com.craftpilot.analyticsservice;

import com.craftpilot.analyticsservice.config.LightSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; 
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableDiscoveryClient 
@EnableWebFlux
@Import(LightSecurityConfig.class)
@EnableRetry
@OpenAPIDefinition(info = @Info(
    title = "Analytics Service API",
    version = "1.0",
    description = "Service for managing analytics data for Craft Pilot"
))
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}