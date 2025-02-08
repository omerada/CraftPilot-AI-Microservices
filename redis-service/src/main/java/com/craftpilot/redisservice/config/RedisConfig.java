package com.craftpilot.redisservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        
        SingleServerConfig serverConfig = config.useSingleServer()
            .setAddress(address)
            .setConnectionMinimumIdleSize(1)
            .setConnectionPoolSize(2)
            .setRetryAttempts(5)
            .setRetryInterval(1500)
            .setTimeout(3000)
            .setConnectTimeout(3000)
            .setDnsMonitoringInterval(5000);

        if (StringUtils.hasText(redisPassword)) {
            log.info("Redis şifresi yapılandırılıyor...");
            serverConfig.setUsername("default");  // Redis 6+ için default kullanıcı
            serverConfig.setPassword(redisPassword);
        }

        try {
            log.info("Redis'e bağlanılıyor: {}", address);
            RedissonClient client = Redisson.create(config);
            // Test bağlantıyı
            client.getBucket("test").delete();
            log.info("Redis bağlantısı başarılı");
            return client;
        } catch (Exception e) {
            log.error("Redis bağlantısı kurulamadı: {}", e.getMessage(), e);
            throw new RuntimeException("Redis bağlantısı kurulamadı: " + e.getMessage(), e);
        }
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
} 