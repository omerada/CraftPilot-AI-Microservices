package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Not: Redis ile ilgili devre kesici ayarları artık redis-client-lib 
 * tarafından yapılandırılmaktadır. Bu sınıf sadece User Service'e özel
 * devre kesici ve retry yapılandırmalarını içermektedir.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .recordExceptions(Exception.class, TimeoutException.class)
                .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
        
        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public CircuitBreaker userPreferencesCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("userPreferences");
    }
    
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();
    }
}
