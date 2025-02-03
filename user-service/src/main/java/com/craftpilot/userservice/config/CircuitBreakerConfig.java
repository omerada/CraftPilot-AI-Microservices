package com.craftpilot.userservice.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfigCustomizer defaultCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("default",
            builder -> builder
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer firebaseCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("firebase",
            builder -> builder
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer redisCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("redis",
            builder -> builder
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer kafkaCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("kafka",
            builder -> builder
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
        );
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerFactoryCustomizer() {
        return factory -> {
            factory.configure(
                builder -> builder
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()),
                "firebase"
            );
            
            factory.configure(
                builder -> builder
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(1))
                        .build()),
                "redis"
            );
            
            factory.configure(
                builder -> builder
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build()),
                "kafka"
            );
        };
    }
} 