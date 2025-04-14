package com.craftpilot.userservice.config;

import com.craftpilot.redis.service.ReactiveCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

/**
 * Redis özelleştirmeleri için konfigürasyon sınıfı.
 * 
 * Not: Bu sınıf redis-client-lib tarafından otomatik olarak sağlanan
 * beanlerle birlikte çalışacak şekilde yapılandırılmıştır.
 */
@Configuration
public class RedisConfig {

    @Value("${craftpilot.redis.cache-ttl-hours:1}")
    private long cacheTtlHours;

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
    
    /**
     * ReactiveCacheService için manuel bean tanımı
     * Bu, redis-client-lib'in otomatik yapılandırması çalışmadığında fallback olarak kullanılır
     */
    @Bean
    @Primary
    public ReactiveCacheService reactiveCacheService(
            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ObjectMapper objectMapper) {
        return new ReactiveCacheService(
                reactiveStringRedisTemplate,
                circuitBreakerRegistry,
                Duration.ofHours(cacheTtlHours),
                objectMapper);
    }
}
