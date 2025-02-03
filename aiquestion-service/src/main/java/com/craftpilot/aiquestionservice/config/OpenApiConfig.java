package com.craftpilot.aiquestionservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "AI Question Service API",
        version = "1.0.0",
        description = "Craft Pilot platformu için yapay zeka destekli soru üretimi ve yönetimi servisi",
        contact = @Contact(
            name = "Craft Pilot Team",
            email = "support@craftpilot.ai",
            url = "https://craftpilot.ai"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8082",
            description = "Local Development Server"
        ),
        @Server(
            url = "https://api.craftpilot.ai",
            description = "Production Server"
        )
    }
)
public class OpenApiConfig {
} 