package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.password:13579ada}")
    private String redisPassword;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(Integer.parseInt(redisPort));
        config.setPassword(redisPassword);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserEntity> userRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        
        Jackson2JsonRedisSerializer<UserEntity> serializer = new Jackson2JsonRedisSerializer<>(mapper, UserEntity.class);
        RedisSerializationContext<String, UserEntity> context = createSerializationContext(serializer);
        
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        
        Jackson2JsonRedisSerializer<UserPreference> serializer = new Jackson2JsonRedisSerializer<>(mapper, UserPreference.class);
        RedisSerializationContext<String, UserPreference> context = createSerializationContext(serializer);
        
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    private <T> RedisSerializationContext<String, T> createSerializationContext(
            Jackson2JsonRedisSerializer<T> serializer) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        return RedisSerializationContext.<String, T>newSerializationContext(keySerializer)
                .value(serializer)
                .hashKey(keySerializer)
                .hashValue(serializer)
                .build();
    }
}
