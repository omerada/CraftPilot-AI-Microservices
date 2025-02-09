package com.craftpilot.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public RouteLocator swaggerRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("swagger_route", r -> r
                        .path("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .uri("http://localhost:8080"))
                .build();
    }
}
