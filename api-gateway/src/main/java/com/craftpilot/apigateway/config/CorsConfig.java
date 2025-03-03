package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://*.craftpilot.io",
                "https://craftpilot.io"
            )
            .allowedMethods("*")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count", "X-Error-Message")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
