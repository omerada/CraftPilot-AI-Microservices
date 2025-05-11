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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableDiscoveryClient 
@EnableWebFlux
@Import(LightSecurityConfig.class)
@EnableRetry
@EnableAsync
@Slf4j
@OpenAPIDefinition(info = @Info(
    title = "Analytics Service API",
    version = "1.0",
    description = "Service for managing analytics data for Craft Pilot"
))
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
    
    @Bean
    public ApplicationRunner logEnvironment() {
        return args -> {
            log.info("Analytics Service started with the following configuration:");
            log.info("Kafka Enabled: {}", System.getProperty("kafka.enabled", 
                    System.getenv().getOrDefault("KAFKA_ENABLED", "true")));
            log.info("Kafka Bootstrap Servers: {}", System.getProperty("kafka.bootstrap-servers", 
                    System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")));
            log.info("MongoDB URI: {}", maskSensitiveInfo(System.getProperty("spring.data.mongodb.uri", 
                    System.getenv().getOrDefault("MONGODB_URI", "mongodb://mongodb:27017/analytics"))));
        };
    }
    
    private String maskSensitiveInfo(String uri) {
        // Simple masking for MongoDB URI to avoid exposing credentials in logs
        return uri.replaceAll("mongodb\\+srv://.*:.*@", "mongodb+srv://***:***@")
                 .replaceAll("mongodb://.*:.*@", "mongodb://***:***@");
    }
}