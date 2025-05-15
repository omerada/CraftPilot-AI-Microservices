package com.craftpilot.redis;

import com.craftpilot.redis.config.RedisClientProperties;
import com.craftpilot.redis.connection.ReactiveRedisConnectionProvider;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import com.craftpilot.redis.repository.ReactiveRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

@Configuration
@Slf4j
@ConditionalOnClass(ReactiveRedisTemplate.class)
public class RedisClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "redis.client")
    public RedisClientProperties redisClientProperties() {
        return RedisClientProperties.builder()
                .host("localhost")
                .port(6379)
                .database(0)
                .connectTimeout(Duration.ofSeconds(3))
                .commandTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisConnectionProvider reactiveRedisConnectionProvider(
            ReactiveRedisConnectionFactory connectionFactory,
            RedisClientProperties properties) {
        
        log.info("Creating ReactiveRedisConnectionProvider bean");
        return new ReactiveRedisConnectionProvider(connectionFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public RedisMetricsService redisMetricsService(
            MeterRegistry meterRegistry,
            ReactiveRedisService redisService) {
        
        log.info("Creating RedisMetricsService bean with MeterRegistry");
        return new RedisMetricsService(meterRegistry, redisService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisService reactiveRedisService(
            ReactiveRedisTemplate<String, Object> redisTemplate) {
        
        log.info("Creating ReactiveRedisService bean");
        return new ReactiveRedisService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveCacheService reactiveCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            RedisClientProperties properties) {
        
        log.info("Creating ReactiveCacheService bean");
        return new ReactiveCacheService(
                redisTemplate,
                properties.getCacheTtl() != null ? properties.getCacheTtl() : Duration.ofMinutes(30)
        );
    }

    // Circuit Breaker integrated versions (optional)
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CircuitBreakerRegistry.class)
    @ConditionalOnProperty(name = "redis.client.circuit-breaker.enabled", havingValue = "true")
    public ReactiveCacheService circuitBreakerEnabledCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RedisClientProperties properties) {
        
        log.info("Creating CircuitBreaker-enabled ReactiveCacheService bean");
        return new ReactiveCacheService(
                redisTemplate,
                circuitBreakerRegistry,
                properties.getCacheTtl() != null ? properties.getCacheTtl() : Duration.ofMinutes(30)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisRepository reactiveRedisRepository(
            ReactiveRedisTemplate<String, Object> redisTemplate) {
        
        log.info("Creating ReactiveRedisRepository bean");
        return new ReactiveRedisRepository(redisTemplate);
    }
}
