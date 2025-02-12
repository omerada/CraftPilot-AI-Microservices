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
    public ReactiveRedisTemplate<String, UserEntity> userEntityRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        
        Jackson2JsonRedisSerializer<UserEntity> serializer = new Jackson2JsonRedisSerializer<>(mapper, UserEntity.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserEntity> builder =
            RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, UserEntity> context = builder
            .value(serializer)
            .hashKey(keySerializer)
            .hashValue(serializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserPreference> userPreferenceRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        
        Jackson2JsonRedisSerializer<UserPreference> serializer = new Jackson2JsonRedisSerializer<>(mapper, UserPreference.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserPreference> builder =
            RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, UserPreference> context = builder
            .value(serializer)
            .hashKey(keySerializer)
            .hashValue(serializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
