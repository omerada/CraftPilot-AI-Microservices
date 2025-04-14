package com.craftpilot.redis.config;

import com.craftpilot.redis.connection.ReactiveRedisConnectionProvider;
import com.craftpilot.redis.health.RedisHealthIndicator;
import com.craftpilot.redis.lock.DistributedLockService;
import com.craftpilot.redis.lock.ReactiveRedissonLockService;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(RedisClientProperties.class)
public class RedisClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisConnectionProvider reactiveRedisConnectionProvider(RedisClientProperties properties) {
        return new ReactiveRedisConnectionProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(
            ReactiveRedisConnectionProvider connectionProvider) {
        return connectionProvider.createConnectionFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(RedisClientProperties properties) {
        // Bu konfigürasyon, tüm projeler için merkezi devre kesici konfigürasyonu olacak
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(properties.getCircuitBreakerFailureRateThreshold())
                .waitDurationInOpenState(properties.getCircuitBreakerWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.getCircuitBreakerPermittedCallsInHalfOpenState())
                .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedisClientProperties properties) {
        Config config = new Config();
        String address = String.format("redis://%s:%d", properties.getHost(), properties.getPort());
        
        SingleServerConfig serverConfig = config.useSingleServer()
            .setAddress(address)
            .setDatabase(properties.getDatabase())
            .setConnectionMinimumIdleSize(properties.getPoolMinIdle())
            .setConnectionPoolSize(properties.getPoolMaxActive())
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setTimeout((int) Math.min(properties.getTimeout().toMillis(), Integer.MAX_VALUE))
            .setConnectTimeout((int) Math.min(properties.getConnectTimeout().toMillis(), Integer.MAX_VALUE));

        if (StringUtils.hasText(properties.getPassword())) {
            serverConfig.setPassword(properties.getPassword());
        }
        
        return Redisson.create(config);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ReactiveRedisService reactiveRedisService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RedisClientProperties properties) {
        // "redis" ismindeki devre kesici burada yaratılıyor
        // Bu isim, tüm mikroservislerde tek bir isim olacak
        return new ReactiveRedisService(
                redisTemplate, 
                circuitBreakerRegistry,
                Duration.ofHours(properties.getCacheTtlHours()));
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean
    public ReactiveCacheService reactiveCacheService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RedisClientProperties properties,
            ObjectMapper objectMapper) {
        return new ReactiveCacheService(
                redisTemplate, 
                circuitBreakerRegistry,
                Duration.ofHours(properties.getCacheTtlHours()),
                objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockService distributedLockService(
            RedissonClient redissonClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ReactiveRedissonLockService(redissonClient, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public RedisMetricsService redisMetricsService(
            MeterRegistry meterRegistry, 
            ReactiveRedisService redisService) {
        return new RedisMetricsService(meterRegistry, redisService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "craftpilot.redis", name = "health-indicator-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RedisHealthIndicator redisHealthIndicator(ReactiveRedisService redisService) {
        return new RedisHealthIndicator(redisService);
    }
}
