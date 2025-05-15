package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

        @Bean
        @Primary
        public CircuitBreakerRegistry mainCircuitBreakerRegistry() {
                return CircuitBreakerRegistry.of(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                                .slidingWindowSize(10)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofSeconds(10))
                                .permittedNumberOfCallsInHalfOpenState(5)
                                .build());
        }

        @Bean
        public TimeLimiterConfig timeLimiterConfig() {
                return TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(5))
                                .build();
        }
}