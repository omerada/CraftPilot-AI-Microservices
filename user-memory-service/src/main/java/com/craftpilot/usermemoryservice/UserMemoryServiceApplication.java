package com.craftpilot.usermemoryservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title = "User Memory Service API",
        version = "1.0.0",
        description = "Service for managing user memory and information extraction"
    )
)
public class UserMemoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserMemoryServiceApplication.class, args);
    }
}
