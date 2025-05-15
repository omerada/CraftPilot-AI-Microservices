package com.craftpilot.userservice.config;

import com.craftpilot.redis.config.RedisClientProperties;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * User Service için özel Redis yapılandırması
 * Bu sınıf, redis-client-lib'nin sağladığı temel altyapıya ek olarak
 * User service'e özel Redis yapılandırmalarını sağlar.
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * Redis Cache Manager yapılandırması
     * User service için özelleştirilmiş cache yapılandırması sağlar
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis Cache Manager for user-service");
        
        // User service için özel cache yapılandırması
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .prefixCacheNameWith("user-service:");
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }

    /**
     * Redis client properties için özelleştirme
     * Gerekirse redis-client-lib'nin sağladığı özelliklere ek özellikler eklenebilir
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public RedisClientProperties redisClientProperties() {
        log.info("Creating custom RedisClientProperties for user-service");
        
        return RedisClientProperties.builder()
                // Properties will be loaded from application.yml
                .build();
    }
    
    /**
     * ReactiveRedisTemplate beanini oluşturur
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        log.info("Creating ReactiveRedisTemplate for user-service");
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, Object> context = builder
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
    
    /**
     * Redis sağlık kontrolü için health indicator
     * Bean adı benzersiz olacak şekilde değiştirildi
     */
    @Bean
    public ReactiveHealthIndicator userServiceRedisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
        return () -> connectionFactory.getReactiveConnection().ping()
                .map(ping -> Health.up()
                        .withDetail("ping", ping)
                        .withDetail("service", "user-service")
                        .build())
                .onErrorResume(e -> Mono.just(Health.down()
                        .withException(e)
                        .withDetail("service", "user-service")
                        .build()));
    }
}
