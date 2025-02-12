package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public ReactiveRedisOperations<String, UserPreference> redisOperations(
            ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        
        Jackson2JsonRedisSerializer<UserPreference> serializer = new Jackson2JsonRedisSerializer<>(mapper, UserPreference.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, UserPreference> builder =
                RedisSerializationContext.newSerializationContext(stringSerializer);

        RedisSerializationContext<String, UserPreference> context = builder
                .value(serializer)
                .hashKey(stringSerializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserEntity> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<UserEntity> valueSerializer = 
            new Jackson2JsonRedisSerializer<>(UserEntity.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserEntity> builder =
            RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, UserEntity> context = builder
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
