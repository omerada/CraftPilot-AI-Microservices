package com.craftpilot.userservice.config;

import com.craftpilot.redis.config.RedisClientProperties;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
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
        public RedisCacheManager redisCacheManager(
                        @Qualifier("craftPilotRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
                log.info("Configuring Redis Cache Manager for user-service");

                // User service için özel cache yapılandırması
                RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer()))
                                .prefixCacheNameWith("user-service:");

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(cacheConfig)
                                .build();
        }

        /**
         * Redis Health Indicator
         * User service için özel health indicator yapılandırması
         */
        @Bean("userServiceRedisHealthIndicator")
        public ReactiveHealthIndicator userServiceRedisHealthIndicator(
                        @Qualifier("craftPilotReactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {
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
