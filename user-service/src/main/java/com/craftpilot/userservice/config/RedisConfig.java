package com.craftpilot.userservice.config;

import com.craftpilot.redis.RedisClientAutoConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
}
