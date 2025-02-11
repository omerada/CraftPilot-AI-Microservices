package com.craftpilot.notificationservice.config;

import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.model.NotificationPreference;
import com.craftpilot.notificationservice.model.NotificationTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:13579ada}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:10000}")
    private long timeout;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);
        
        ClientOptions clientOptions = ClientOptions.builder()
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .autoReconnect(true)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(timeout)))
            .protocolVersion(ProtocolVersion.RESP2)
            .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofMillis(timeout))
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public ReactiveRedisTemplate<String, Notification> notificationRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Notification> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(Notification.class);

        RedisSerializationContext<String, Notification> context = RedisSerializationContext
                .<String, Notification>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationPreference> preferenceRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<NotificationPreference> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationPreference.class);

        RedisSerializationContext<String, NotificationPreference> context = RedisSerializationContext
                .<String, NotificationPreference>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, NotificationTemplate> templateRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<NotificationTemplate> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationTemplate.class);

        RedisSerializationContext<String, NotificationTemplate> context = RedisSerializationContext
                .<String, NotificationTemplate>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}