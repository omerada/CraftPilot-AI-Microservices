package com.craftpilot.llmservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory() {
        return new ReactiveResilience4JCircuitBreakerFactory();
    }

    @Bean
    public ReactiveCircuitBreaker openRouterCircuitBreaker(ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return circuitBreakerFactory.create("openRouter");
    }

    @Bean
    public org.springframework.cloud.circuitbreaker.resilience4j.Customizer<ReactiveResilience4JCircuitBreakerFactory> openRouterCustomizer() {
        return factory -> factory.configure(builder -> builder
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .build()), "openRouter");
    }
}
