package com.craftpilot.userservice.config;

import com.craftpilot.redis.config.RedisClientAutoConfiguration;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveRedisService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

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
