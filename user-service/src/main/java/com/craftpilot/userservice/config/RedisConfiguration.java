package com.craftpilot.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Value("${spring.redis.host:redis}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Creating Redis connection factory with host: {}, port: {}", redisHost, redisPort);
        
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            configuration.setPassword(redisPassword);
        }
        configuration.setDatabase(redisDatabase);
        
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        log.info("Creating ReactiveRedisTemplate with connection factory");
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        
        RedisSerializationContext<String, Object> context = builder
                .value(serializer)
                .hashValue(serializer)
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
