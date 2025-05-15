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
    private Duration connectTimeout;
    
    /**
     * Redis komut timeout
     */
    private Duration commandTimeout;
}
