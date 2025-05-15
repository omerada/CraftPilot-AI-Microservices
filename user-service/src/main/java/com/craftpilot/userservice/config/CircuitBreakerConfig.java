package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

    @Value("${resilience4j.circuitbreaker.slidingWindowSize:10}")
    private int slidingWindowSize;

    @Value("${resilience4j.circuitbreaker.failureRateThreshold:50}")
    private float failureRateThreshold;

    @Value("${resilience4j.circuitbreaker.waitDurationInOpenState:10000}")
    private long waitDurationInOpenState;

    @Value("${resilience4j.circuitbreaker.permittedNumberOfCallsInHalfOpenState:5}")
    private int permittedNumberOfCallsInHalfOpenState;

    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                .custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .build();
        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        return RateLimiterRegistry.of(rateLimiterConfig);
    }

    @Bean
    @Primary
    public CircuitBreakerConfigCustomizer userServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
                .of("userService", builder -> builder
                        .slidingWindowSize(10)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .waitDurationInOpenState(Duration.ofSeconds(50))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .recordExceptions(Exception.class)
                        .ignoreExceptions(
                                com.craftpilot.userservice.exception.NotFoundException.class,
                                com.craftpilot.userservice.exception.ValidationException.class));
    }

    @Bean
    public CircuitBreakerConfigCustomizer redisCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
                .of("userRedis", builder -> builder
                        .slidingWindowSize(10)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .recordExceptions(Exception.class));
    }
}