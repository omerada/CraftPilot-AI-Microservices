package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

@Configuration
public class RedisConfig {

    // Change these property names to match the standard Spring Boot Redis properties
    @Value("${spring.data.redis.host:redis}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;
    
    @Value("${spring.data.redis.password:13579ada}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:10000}")
    private long timeout;
    
    @Value("${spring.data.redis.lettuce.pool.max-active:16}")
    private int maxActive;
    
    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;
    
    @Value("${spring.data.redis.lettuce.pool.min-idle:4}")
    private int minIdle;
    
    @Value("${spring.data.redis.lettuce.pool.max-wait:-1}")
    private long maxWait;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        redisConfig.setPassword(redisPassword);
        
        // Connection pool configuration
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        
        // Socket and client options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(timeout))
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserEntity> userRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<UserEntity> valueSerializer = new Jackson2JsonRedisSerializer<>(UserEntity.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, UserEntity> builder = 
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, UserEntity> context = 
                builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
    
    @Bean
    public ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<UserPreference> valueSerializer = new Jackson2JsonRedisSerializer<>(UserPreference.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, UserPreference> builder = 
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, UserPreference> context = 
                builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
