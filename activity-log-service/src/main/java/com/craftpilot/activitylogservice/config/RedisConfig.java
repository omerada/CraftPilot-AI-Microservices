package com.craftpilot.activitylogservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import java.time.Duration;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "redis", matchIfMissing = false)
public class RedisConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.connect-timeout:2000}")
    private long connectTimeout;

    @Value("${spring.data.redis.timeout:5000}")
    private long commandTimeout;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        log.info("Configuring Redis connection factory with host: {}, port: {}", redisHost, redisPort);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(RedisPassword.of(redisPassword));
            log.debug("Redis password configured successfully");
        }
        
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();
        
        ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .build();
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofMillis(commandTimeout))
            .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        log.debug("Configuring ReactiveRedisTemplate");
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
                
        RedisSerializationContext<String, Object> context = builder
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
                
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveHealthIndicator redisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
        return () -> {
            log.debug("Checking Redis health");
            return connectionFactory.getReactiveConnection().ping()
                    .timeout(Duration.ofSeconds(2))
                    .map(pong -> {
                        log.debug("Redis health check successful: {}", pong);
                        return Health.up()
                                .withDetail("ping", pong)
                                .withDetail("host", redisHost)
                                .build();
                    })
                    .onErrorResume(e -> {
                        log.warn("Redis health check failed: {}", e.getMessage());
                        return Mono.just(Health.down()
                                .withException(e)
                                .withDetail("host", redisHost)
                                .build());
                    });
        };
    }
}
