package com.craftpilot.userservice.config;

import com.craftpilot.redis.RedisClientAutoConfiguration;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveRedisService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import java.time.Duration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigCustomizer;

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
            .of("redisCircuitBreaker", builder -> builder
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10)) // 30 saniyeden 10'a düşürüldü
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(3) // 5'ten 3'e düşürüldü daha hızlı tepki vermesi için
                .recordExceptions(Exception.class)
            );
    }
    
    /**
     * ReactiveRedisService için primary bean tanımı.
     * Bu, ReactiveCacheService ile çakışmaları önler.
     */
    @Bean 
    @Primary
    public ReactiveRedisService primaryRedisService(ReactiveRedisService reactiveRedisService) {
        return reactiveRedisService;
    }
    
    /**
     * RedisMetricsService için özel bir bean tanımı yapmak yerine
     * varolan RedisMetricsService bean'ini override ediyoruz ve doğrudan
     * primary olan ReactiveRedisService bean'ini kullanmasını sağlıyoruz.
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
