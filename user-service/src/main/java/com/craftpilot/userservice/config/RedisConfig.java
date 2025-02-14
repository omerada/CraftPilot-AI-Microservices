package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
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

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private String redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(Integer.parseInt(redisPort));
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<UserPreference> valueSerializer =
                new Jackson2JsonRedisSerializer<>(UserPreference.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, UserPreference> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, UserPreference> context =
                builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
