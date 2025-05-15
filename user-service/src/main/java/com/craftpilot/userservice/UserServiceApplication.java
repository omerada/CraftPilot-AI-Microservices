package com.craftpilot.userservice;

import com.craftpilot.userservice.config.CircuitBreakerConfig;
import com.craftpilot.userservice.config.LightSecurityConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import({ LightSecurityConfig.class, CircuitBreakerConfig.class })
@OpenAPIDefinition(info = @Info(title = "User Service API", version = "1.0", description = "Service for managing user operations and data"))
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
