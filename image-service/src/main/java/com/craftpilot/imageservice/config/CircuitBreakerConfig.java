package com.craftpilot.imageservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Configuration
@Slf4j
public class CircuitBreakerConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> mongoServiceCustomizer() {
        log.info("Configuring MongoDB circuit breaker");
        return factory -> {
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                            .custom()
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .slidingWindowSize(15)
                            .failureRateThreshold(40)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .recordExceptions(
                                    com.mongodb.MongoTimeoutException.class,
                                    com.mongodb.MongoSocketException.class,
                                    com.mongodb.MongoExecutionTimeoutException.class,
                                    com.mongodb.MongoQueryException.class,
                                    com.mongodb.MongoWriteException.class,
                                    com.mongodb.MongoCommandException.class,
                                    java.net.UnknownHostException.class)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(30))
                            .build()),
                    "mongoService");
        };
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> kafkaServiceCustomizer() {
        log.info("Configuring Kafka circuit breaker");
        return factory -> {
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                            .custom()
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .slidingWindowSize(15)
                            .failureRateThreshold(45)
                            .waitDurationInOpenState(Duration.ofSeconds(60))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()),
                    "kafkaService");
        };
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> redisServiceCustomizer() {
        log.info("Configuring Redis circuit breaker");
        return factory -> {
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                            .custom()
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .slidingWindowSize(15)
                            .failureRateThreshold(45)
                            .waitDurationInOpenState(Duration.ofSeconds(15))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(2))
                            .build()),
                    "redisService");
        };
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        log.info("Configuring default circuit breaker");
        return factory -> {
            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                            .custom()
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .slidingWindowSize(10)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(10))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(3))
                            .build())
                    .build());
        };
    }
}
