package com.craftpilot.userservice.config;

import com.craftpilot.redis.config.RedisClientProperties;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import java.time.Duration;

/**
 * User Service için özel Redis yapılandırması
 * Bu sınıf, redis-client-lib'nin sağladığı temel altyapıya ek olarak
 * User service'e özel Redis yapılandırmalarını sağlar.
 */
@Configuration
@Slf4j
public class RedisConfiguration {

        @Value("${spring.redis.host:redis}")
        private String redisHost;

        @Value("${spring.redis.port:6379}")
        private int redisPort;

        @Value("${spring.redis.password:}")
        private String redisPassword;

        @Value("${spring.redis.connect-timeout:2000}")
        private long connectTimeout;

        @Value("${spring.redis.command-timeout:1000}")
        private long commandTimeout;

        @Bean(name = "craftPilotRedisConnectionFactory")
        public RedisConnectionFactory craftPilotRedisConnectionFactory() {
                log.info("Creating custom Redis connection factory for user-service with host: {}, port: {}", redisHost,
                                redisPort);

                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
                config.setHostName(redisHost);
                config.setPort(redisPort);

                if (redisPassword != null && !redisPassword.isEmpty()) {
                        config.setPassword(RedisPassword.of(redisPassword));
                }

                SocketOptions socketOptions = SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(connectTimeout))
                                .build();

                ClientOptions clientOptions = ClientOptions.builder()
                                .socketOptions(socketOptions)
                                .build();

                LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                                .clientOptions(clientOptions)
                                .commandTimeout(Duration.ofMillis(commandTimeout))
                                .build();

                return new LettuceConnectionFactory(config, clientConfig);
        }

        @Bean(name = "craftPilotReactiveRedisConnectionFactory")
        @Primary
        public ReactiveRedisConnectionFactory craftPilotReactiveRedisConnectionFactory() {
                log.info("Creating custom Reactive Redis connection factory for user-service");
                return (ReactiveRedisConnectionFactory) craftPilotRedisConnectionFactory();
        }

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
                                                .withDetail("host", redisHost)
                                                .withDetail("port", redisPort)
                                                .build())
                                .onErrorResume(e -> {
                                        log.error("Redis health check failed: {}", e.getMessage());
                                        return Mono.just(Health.down()
                                                        .withException(e)
                                                        .withDetail("service", "user-service")
                                                        .withDetail("host", redisHost)
                                                        .withDetail("port", redisPort)
                                                        .build());
                                });
        }
}
