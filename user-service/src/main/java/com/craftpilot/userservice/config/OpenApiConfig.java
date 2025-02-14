package com.craftpilot.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "User Preferences API",
        version = "1.0",
        description = "API for managing user preferences"
    )
)
public class OpenApiConfig {
}