package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, UserEntity> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {
        
        Jackson2JsonRedisSerializer<UserEntity> serializer = new Jackson2JsonRedisSerializer<>(UserEntity.class);
        serializer.setObjectMapper(objectMapper);

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserEntity> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, UserEntity> context = builder
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserPreference> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<UserPreference> valueSerializer = 
            new Jackson2JsonRedisSerializer<>(UserPreference.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserPreference> builder =
            RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, UserPreference> context = builder
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