package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfigCustomizer userServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("userService",
                builder -> builder
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .minimumNumberOfCalls(10)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer redisCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("redis",
                builder -> builder
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .minimumNumberOfCalls(10)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer kafkaCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("kafka",
                builder -> builder
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .minimumNumberOfCalls(10)
        );
    }

    @Bean
    public CircuitBreakerConfigCustomizer defaultCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("default",
                builder -> builder
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .minimumNumberOfCalls(10)
        );
    }

    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory() {
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
        
        factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build())
                .build(), "userService");
        
        factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(1))
                        .build())
                .build(), "redis");
        
        factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .build(), "kafka");
        
        return factory;
    }
} 