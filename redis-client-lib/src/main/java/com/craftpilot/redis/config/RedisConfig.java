package com.craftpilot.redis.config;

import com.craftpilot.redis.repository.ReactiveRedisRepository;
import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.redis.service.ReactiveRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisClientProperties properties;

    @Bean(name = "craftPilotRedisConnectionFactory")
    @ConditionalOnMissingBean(name = "craftPilotRedisConnectionFactory")
    public RedisConnectionFactory craftPilotRedisConnectionFactory() {
        log.info("Configuring primary RedisConnectionFactory with host: {}, port: {}", properties.getHost(), properties.getPort());
        
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getHost());
        redisConfig.setPort(properties.getPort());
        redisConfig.setDatabase(properties.getDatabase());
        
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(properties.getPassword()));
        }
        
        if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
            redisConfig.setUsername(properties.getUsername());
        }
        
        // Socket options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        
        // Client options
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder;
        
        // Configure pooling if enabled
        if (properties.getPool().isEnabled()) {
            GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(properties.getPool().getMaxActive());
            poolConfig.setMaxIdle(properties.getPool().getMaxIdle());
            poolConfig.setMinIdle(properties.getPool().getMinIdle());
            poolConfig.setMaxWait(properties.getPool().getMaxWait());
            
            builder = LettucePoolingClientConfiguration.builder()
                    .poolConfig(poolConfig);
        } else {
            builder = LettuceClientConfiguration.builder();
        }
        
        // Set common configuration
        LettuceClientConfiguration clientConfig = builder
                .clientOptions(clientOptions)
                .commandTimeout(properties.getCommandTimeout())
                .build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean(name = "craftPilotReactiveRedisConnectionFactory") 
    @ConditionalOnMissingBean(name = "craftPilotReactiveRedisConnectionFactory")
    @Primary 
    public ReactiveRedisConnectionFactory craftPilotReactiveRedisConnectionFactory() {
        // Aynı yapılandırma ile Reactive bağlantı fabrikası oluştur
        log.info("Configuring primary ReactiveRedisConnectionFactory with host: {}, port: {}", properties.getHost(), properties.getPort());
        return (ReactiveRedisConnectionFactory) craftPilotRedisConnectionFactory();
    }

    @Bean(name = "redisClientReactiveRedisTemplate") // Bean adını daha belirleyici yapıyoruz
    @ConditionalOnMissingBean(name = "redisClientReactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
        @Qualifier("craftPilotReactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        
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
    @ConditionalOnMissingBean(name = "reactiveRedisService") 
    public ReactiveRedisService reactiveRedisService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        log.info("Creating ReactiveRedisService");
        return new ReactiveRedisService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(name = "reactiveRedisRepository")
    public ReactiveRedisRepository reactiveRedisRepository(ReactiveRedisTemplate<String, Object> redisTemplate) {
        log.info("Creating ReactiveRedisRepository");
        return new ReactiveRedisRepository(redisTemplate);
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "reactiveCacheService")
    public ReactiveCacheService reactiveCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate) {
        log.info("Creating ReactiveCacheService");
        return new ReactiveCacheService(
                redisTemplate, 
                properties.getCacheTtl() != null ? properties.getCacheTtl() : Duration.ofMinutes(30));
    }
}
