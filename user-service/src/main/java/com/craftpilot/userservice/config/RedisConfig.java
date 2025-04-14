package com.craftpilot.userservice.config;

import com.craftpilot.redis.RedisClientAutoConfiguration;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveRedisService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Redis özelleştirmeleri için konfigürasyon sınıfı.
 * 
 * Not: Bu sınıf redis-client-lib kütüphanesinin otomatik yapılandırmasını import eder
 * ve sadece özel ayarları içerir. Tüm temel Redis bağlantısı ve yapılandırması
 * RedisClientAutoConfiguration tarafından sağlanmaktadır.
 */
@Configuration
@Import(RedisClientAutoConfiguration.class)
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
    
    /**
     * ReactiveRedisService bean'i için primary tanımlaması.
     * Bu, ReactiveCacheService ile çakışmaları önler.
     */
    @Bean 
    @Primary
    public ReactiveRedisService primaryRedisService(ReactiveRedisService reactiveRedisService) {
        return reactiveRedisService;
    }
    
    /**
     * RedisMetricsService bean'i için özelleştirilmiş yapılandırma.
     * Bu, birden fazla ReactiveRedisService tipindeki bean çakışmasını önlemek için
     * primary olan ReactiveRedisService bean'ini kullanmasını sağlar.
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(name = "redisMetricsService")
    public RedisMetricsService redisMetricsService(
            MeterRegistry meterRegistry, 
            ReactiveRedisService primaryRedisService) {
        return new RedisMetricsService(meterRegistry, primaryRedisService);
    }
}
