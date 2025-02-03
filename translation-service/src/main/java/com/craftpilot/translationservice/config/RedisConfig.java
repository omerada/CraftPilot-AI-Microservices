package com.craftpilot.translationservice.config;

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

import com.craftpilot.translationservice.model.Translation;
import com.craftpilot.translationservice.model.TranslationHistory;

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
    public ReactiveRedisTemplate<String, Translation> reactiveRedisTemplateTranslation(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Translation> valueSerializer = new Jackson2JsonRedisSerializer<>(Translation.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, Translation> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, Translation> context = builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, TranslationHistory> reactiveRedisTemplateTranslationHistory(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<TranslationHistory> valueSerializer = new Jackson2JsonRedisSerializer<>(TranslationHistory.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, TranslationHistory> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, TranslationHistory> context = builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
} 