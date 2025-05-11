package com.craftpilot.userservice.config;

import com.craftpilot.redis.config.RedisClientAutoConfiguration;
import com.craftpilot.redis.health.RedisHealthIndicator;
import com.craftpilot.redis.metrics.RedisMetricsService;
import com.craftpilot.redis.service.ReactiveRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis özelleştirmeleri için konfigürasyon sınıfı.
 * 
 * Not: Bu sınıf redis-client-lib kütüphanesinin otomatik yapılandırmasını import eder
 * ve sadece özel ayarları içerir. Tüm temel Redis bağlantısı ve yapılandırması
 * RedisClientAutoConfiguration tarafından sağlanmaktadır.
 */
@Configuration
@Import(RedisClientAutoConfiguration.class)
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofSeconds(1))
                .build();
                
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        StringRedisSerializer valueSerializer = new StringRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, String> context = builder
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * ReactiveRedisService için özel bean tanımı.
     * Bu artık @Primary olarak işaretlenmemiştir, çünkü birden fazla Primary bean olması çakışmalara yol açıyor.
     */
    @Bean 
    public ReactiveRedisService primaryRedisService(ReactiveRedisService reactiveRedisService) {
        return reactiveRedisService;
    }
    
    /**
     * RedisMetricsService için özel bir bean tanımı yapmak yerine
     * varolan RedisMetricsService bean'ini override ediyoruz ve doğrudan
     * primary olan ReactiveRedisService bean'ini kullanmasını sağlıyoruz.
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(name = "redisMetricsService")
    public RedisMetricsService redisMetricsService(
            MeterRegistry meterRegistry, 
            ReactiveRedisService primaryRedisService) {
        return new RedisMetricsService(meterRegistry, primaryRedisService);
    }
    
    /**
     * RedisHealthIndicator için özel bir bean tanımı yapıyoruz
     * ve primaryRedisService bean'ini açıkça belirtiyoruz.
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisHealthIndicator")
    public RedisHealthIndicator redisHealthIndicator(ReactiveRedisService primaryRedisService) {
        return new RedisHealthIndicator(primaryRedisService);
    }
}
