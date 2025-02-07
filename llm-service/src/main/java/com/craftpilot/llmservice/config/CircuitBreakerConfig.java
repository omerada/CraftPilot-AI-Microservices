package com.craftpilot.llmservice.config;
 
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfigCustomizer openRouterCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer
                .of("openRouter",
                        builder -> builder
                                .slidingWindowSize(10)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                                .permittedNumberOfCallsInHalfOpenState(3)
                                .minimumNumberOfCalls(5)
                );
    }

    @Bean
    public ReactiveCircuitBreaker openRouterCircuitBreaker(
            ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return circuitBreakerFactory.create("openRouter");
    }
} 