package com.craftpilot.codeservice.config;

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

import com.craftpilot.codeservice.model.Code;
import com.craftpilot.codeservice.model.CodeHistory;

@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        configuration.setPassword(redisPassword);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    public ReactiveRedisTemplate<String, Code> reactiveRedisTemplateCode(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Code> valueSerializer = new Jackson2JsonRedisSerializer<>(Code.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, Code> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, Code> context = builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, CodeHistory> reactiveRedisTemplateCodeHistory(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<CodeHistory> valueSerializer = new Jackson2JsonRedisSerializer<>(CodeHistory.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, CodeHistory> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, CodeHistory> context = builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }
} 