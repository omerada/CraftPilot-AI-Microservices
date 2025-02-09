package com.craftpilot.apigateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // User Service Routes
                .route("user-service", r -> r.path("/api/users/**", "/api/profiles/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("userServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/user")))
                        .uri("lb://user-service"))

                // Admin Service Routes
                .route("admin-service", r -> r.path("/api/admin/**", "/api/management/**", "/api/system/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("adminServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/admin")))
                        .uri("lb://admin-service"))

                // LLM Service Routes
                .route("llm-service", r -> r.path("/api/questions/**", "/api/chat/**", "/api/completions/**", "/api/prompts/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("llmServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/llm")))
                        .uri("lb://llm-service"))

                // Image Service Routes
                .route("image-service", r -> r.path("/api/images/**", "/api/media/**", "/api/gallery/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("imageServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/image")))
                        .uri("lb://image-service"))

                // Analytics Service Routes
                .route("analytics-service", r -> r.path("/api/analytics/**", "/api/reports/**", "/api/metrics/**", "/api/statistics/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("analyticsServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/analytics")))
                        .uri("lb://analytics-service"))

                // Credit Service Routes
                .route("credit-service", r -> r.path("/api/credits/**", "/api/payments/**", "/api/billing/**", "/api/transactions/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("creditServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/credit")))
                        .uri("lb://credit-service"))

                // Subscription Service Routes
                .route("subscription-service", r -> r.path("/api/subscriptions/**", "/api/plans/**", "/api/packages/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("subscriptionServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/subscription")))
                        .uri("lb://subscription-service"))

                // Notification Service Routes
                .route("notification-service", r -> r.path("/api/notifications/**", "/api/messages/**", "/api/emails/**", "/api/push/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("notificationServiceCircuitBreaker")
                                .setFallbackUri("forward:/fallback/notification")))
                        .uri("lb://notification-service"))
                
                .build();
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(5)
            .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return factory -> factory.configure(
            builder -> builder
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig),
            "defaultCircuitBreaker"
        );
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("Authorization") != null ?
            exchange.getRequest().getHeaders().getFirst("Authorization") :
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}