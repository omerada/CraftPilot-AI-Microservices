package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

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
                .recordExceptions(
                    Exception.class
                )
                .ignoreExceptions(
                    com.craftpilot.userservice.exception.NotFoundException.class,
                    com.craftpilot.userservice.exception.ValidationException.class
                )
            );
    }

    @Bean
    public RateLimiterConfigCustomizer userServiceRateLimiterConfig() {
        return RateLimiterConfigCustomizer
            .of("userService", builder -> builder
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(3))
            );
    }
    
    @Bean
    public CircuitBreakerConfigCustomizer redisCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
            .of("redis", builder -> builder
                .slidingWindowSize(10)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .recordExceptions(Exception.class)
            );
    }
}