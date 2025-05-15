package com.craftpilot.redis.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis Client yapılandırma özellikleri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "redis.client")
public class RedisClientProperties {
    
    /**
     * Özel cache key prefix
     */
    private String keyPrefix;
    
    /**
     * Varsayılan cache TTL
     */
    private Duration cacheTtl;
    
    /**
     * Redis kullanımını devre dışı bırak
     */
    private boolean disabled;
    
    /**
     * Redis bağlantı havuzu boyutu
     */
    private Integer poolSize;
    
    /**
     * Redis bağlantı timeout
     */
    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(3);
    
    /**
     * Redis komut timeout
     */
    @Builder.Default
    private Duration commandTimeout = Duration.ofSeconds(5);
    
    /**
     * Redis host
     */
    @Builder.Default
    private String host = "localhost";
    
    /**
     * Redis port
     */
    @Builder.Default
    private int port = 6379;
    
    /**
     * Redis database index
     */
    @Builder.Default
    private int database = 0;
    
    /**
     * Redis username
     */
    private String username;
    
    /**
     * Redis password
     */
    private String password;
    
    /**
     * Circuit breaker configuration
     */
    @Builder.Default
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    
    /**
     * Havuz yapılandırması
     */
    @Builder.Default
    private Pool pool = new Pool();
    
    /**
     * Retry yapılandırması
     */
    @Builder.Default
    private Retry retry = new Retry();
    
    /**
     * Circuit breaker configuration
     */
    @Data
    public static class CircuitBreaker {
        /**
         * Whether to enable circuit breaker
         */
        private boolean enabled = false;
        
        /**
         * Name of the circuit breaker
         */
        private String name = "redis";
    }
    
    /**
     * Redis bağlantı havuzu yapılandırması
     */
    @Data
    public static class Pool {
        /**
         * Havuz kullanımını etkinleştir
         */
        private boolean enabled = true;
        
        /**
         * Maksimum aktif bağlantı sayısı
         */
        private int maxActive = 8;
        
        /**
         * Maksimum boşta bekleyen bağlantı sayısı
         */
        private int maxIdle = 8;
        
        /**
         * Minimum boşta bekleyen bağlantı sayısı
         */
        private int minIdle = 0;
        
        /**
         * Bağlantı havuzu tükendiğinde maksimum bekleme süresi
         */
        private Duration maxWait = Duration.ofMillis(1000);
    }
    
    /**
     * Retry yapılandırması
     */
    @Data
    public static class Retry {
        /**
         * Retry mekanizmasını etkinleştir
         */
        private boolean enabled = true;
        
        /**
         * Maksimum deneme sayısı
         */
        private int maxAttempts = 3;
        
        /**
         * Denemeler arası bekleme süresi
         */
        private Duration backoff = Duration.ofMillis(1000);
    }
}
