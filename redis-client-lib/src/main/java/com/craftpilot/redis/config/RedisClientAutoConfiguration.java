package com.craftpilot.redis.config;

import com.craftpilot.redis.connection.ReactiveRedisConnectionProvider;
import com.craftpilot.redis.health.RedisHealthIndicator;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import com.craftpilot.redis.repository.ReactiveRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(ReactiveRedisTemplate.class)
@EnableConfigurationProperties(RedisClientProperties.class)
@Import(RedisConfig.class)
@Slf4j
public class RedisClientAutoConfiguration {

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
    public RedisHealthIndicator redisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
        log.info("Creating RedisHealthIndicator bean");
        return new RedisHealthIndicator(connectionFactory);
    }
    
    // Diğer bean'ler RedisConfig sınıfında tanımlanmış durumda
}
