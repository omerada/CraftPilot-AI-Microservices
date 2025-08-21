package com.craftpilot.redis;

import com.craftpilot.redis.config.RedisConfig;
import com.craftpilot.redis.connection.ReactiveRedisConnectionProvider;
import com.craftpilot.redis.config.RedisClientProperties;
import com.craftpilot.redis.repository.ReactiveRedisRepository;
import com.craftpilot.redis.service.CacheService;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.health.RedisHealthIndicator;
import com.craftpilot.redis.lock.ReactiveRedissonLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

@Configuration
@Import(RedisConfig.class)
public class RedisClientAutoConfiguration {

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private long defaultTtlMillis;

    @Bean
    @ConditionalOnMissingBean
    public RedisClientProperties redisClientProperties(
            @Value("${redis.host:localhost}") String host,
            @Value("${redis.port:6379}") int port,
            @Value("${redis.password:CHANGE_ME_IN_PRODUCTION}") String password,
            @Value("${redis.connect-timeout:2000ms}") Duration connectTimeout,
            @Value("${redis.timeout:1000ms}") Duration timeout) {
        return RedisClientProperties.builder()
                .host(host)
                .port(port)
                .password(password)
                .connectTimeout(connectTimeout)
                .timeout(timeout)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisConnectionProvider reactiveRedisConnectionProvider(RedisClientProperties properties) {
        return new ReactiveRedisConnectionProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedisClientProperties properties) {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setConnectTimeout((int) properties.getConnectTimeout().toMillis())
                .setTimeout((int) properties.getTimeout().toMillis());
        
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            serverConfig.setPassword(properties.getPassword());
        }
        
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisRepository reactiveRedisRepository(ReactiveRedisTemplate<String, Object> redisTemplate) {
        return new ReactiveRedisRepository(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisService reactiveRedisService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ReactiveRedisService(redisTemplate, circuitBreakerRegistry, Duration.ofMillis(defaultTtlMillis));
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveCacheService reactiveCacheService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ObjectMapper objectMapper) {
        return new ReactiveCacheService(
                redisTemplate, 
                circuitBreakerRegistry, 
                Duration.ofMillis(defaultTtlMillis),
                objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedissonLockService reactiveRedissonLockService(
            RedissonClient redissonClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ReactiveRedissonLockService(redissonClient, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisMetricsService redisMetricsService(
            MeterRegistry meterRegistry,
            ReactiveRedisService redisService) {
        return new RedisMetricsService(meterRegistry, redisService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(ReactiveRedisRepository reactiveRedisRepository) {
        return new CacheService(reactiveRedisRepository, Duration.ofMillis(defaultTtlMillis));
    }
}
