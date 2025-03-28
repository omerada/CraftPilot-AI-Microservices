package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)  // SecurityConfig'den önce çalışması için en yüksek önceliği veriyoruz
public class CorsConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://app.craftpilot.io",
                "https://*.craftpilot.io"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count", "X-Error-Message", "X-CSRF-TOKEN")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
