package com.craftpilot.contentservice.config;

import com.craftpilot.contentservice.model.Content;
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
    public ReactiveRedisTemplate<String, Content> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Content> valueSerializer = new Jackson2JsonRedisSerializer<>(Content.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, Content> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, Content> context = builder
                .value(valueSerializer)
                .build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
} 