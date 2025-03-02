package com.craftpilot.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // LLM Service
            .route("llm-service", r -> r
                .path("/ai/**")
                .filters(f -> f
                    .preserveHostHeader()
                    .addResponseHeader("X-Gateway-Route", "llm-service")
                )
                .uri("lb://llm-service"))
                
            // Diğer servisler buraya eklenebilir
            .build();
    }
}
