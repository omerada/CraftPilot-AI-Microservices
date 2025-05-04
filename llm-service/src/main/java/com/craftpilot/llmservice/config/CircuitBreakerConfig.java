package com.craftpilot.llmservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LLM servisi için devre kesici (circuit breaker) yapılandırması
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * OpenRouter API çağrıları için devre kesici yapılandırması
     */
    @Bean
    @Primary
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> openRouterClientCustomizer() {
        return factory -> {
            factory.configure(builder -> builder
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                    .slidingWindowType(SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(10))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(15))
                    .build()), "openRouterClient");
        };
    }
}
