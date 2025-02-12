package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
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
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
