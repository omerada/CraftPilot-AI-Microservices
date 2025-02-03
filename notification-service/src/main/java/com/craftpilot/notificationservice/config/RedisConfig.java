package com.craftpilot.notificationservice.config;

import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.model.NotificationPreference;
import com.craftpilot.notificationservice.model.NotificationTemplate;
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

@Configuration
public class RedisConfig {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    @Bean
    public ReactiveRedisTemplate<String, Notification> notificationRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Notification> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(Notification.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Notification> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Notification> context = 
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationPreference> preferenceRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<NotificationPreference> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationPreference.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, NotificationPreference> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, NotificationPreference> context = 
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationTemplate> templateRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<NotificationTemplate> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationTemplate.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, NotificationTemplate> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, NotificationTemplate> context = 
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
} 