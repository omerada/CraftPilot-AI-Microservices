package com.craftpilot.llmservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class UserMemoryConfig {

    @Bean
    public CircuitBreakerRegistry userMemoryCircuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Arttırıldı
                .permittedNumberOfCallsInHalfOpenState(3)        // Arttırıldı
                .ignoreExceptions(java.net.ConnectException.class, 
                                  org.springframework.web.reactive.function.client.WebClientRequestException.class)
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))  // Arttırıldı
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}
