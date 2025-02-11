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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:13579ada}")
    private String redisPassword;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP2) // Use RESP2 protocol
                .build())
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, Notification> notificationRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
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
            LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<NotificationPreference> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationPreference.class);

        RedisSerializationContext.RedisSerializationContext.RedisSerializationContextBuilder<String, NotificationPreference> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, NotificationPreference> context = 
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationTemplate> templateRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
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