package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Configuration class named {@link GatewayConfig} for setting up API Gateway routes.
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Public endpoints for both web and mobile
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        // Web Auth Endpoints
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/reset-password",
        "/api/auth/verify-email",
        
        // Mobile Auth Endpoints
        "/api/mobile/auth/login",
        "/api/mobile/auth/register",
        "/api/mobile/auth/verify-phone",
        "/api/mobile/auth/refresh-token",
        
        // Health Check
        "/api/health",
        "/api/version"
    );

    /**
     * Configures the route locator to define the routing rules for the gateway.
     *
     * @param builder The RouteLocatorBuilder used to build the RouteLocator.
     * @return A RouteLocator with the defined routes.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("userservice", r -> r.path("/api/users/**", "/api/mobile/users/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                        .setPublicEndpoints(PUBLIC_ENDPOINTS)))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://userservice"))
                
                // Notification Service Routes        
                .route("notificationservice", r -> r.path("/api/notifications/**", "/api/mobile/notifications/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                        .uri("lb://notificationservice"))
                
                // Subscription Service Routes
                .route("subscriptionservice", r -> r.path("/api/subscriptions/**", "/api/mobile/subscriptions/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                        .uri("lb://subscriptionservice"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just(userId);
            }
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }

}
