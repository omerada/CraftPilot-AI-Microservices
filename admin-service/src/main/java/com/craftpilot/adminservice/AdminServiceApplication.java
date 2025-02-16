package com.craftpilot.adminservice;

import com.craftpilot.adminservice.config.LightSecurityConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Admin Service API",
        version = "1.0",
        description = "Admin Service for Craft Pilot AI"
))
@Import(LightSecurityConfig.class)
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}