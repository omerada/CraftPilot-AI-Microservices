package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis özelleştirmeleri için konfigürasyon sınıfı.
 * Not: Temel Redis yapılandırması redis-client-lib tarafından otomatik olarak sağlanmaktadır.
 * Bu sınıf sadece özelleştirmeler içermektedir.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis için devre kesici (circuit breaker) özelleştirmesi
     */
    @Bean
    public CircuitBreakerConfigCustomizer redisCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer
            .of("redis", builder -> builder
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .recordExceptions(Exception.class)
            );
    }
}
