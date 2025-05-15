package com.craftpilot.redis.config;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@Builder
@ConfigurationProperties(prefix = "craftpilot.redis")
public class RedisClientProperties {

    /**
     * Redis sunucu adı
     */
    @Builder.Default
    private String host = "localhost";
    
    /**
     * Redis port numarası
     */
    @Builder.Default
    private int port = 6379;
    
    /**
     * Redis veritabanı indeksi
     */
    @Builder.Default
    private int database = 0;
    
    /**
     * Redis şifresi
     */
    @Builder.Default
    private String password = null;
    
    /**
     * Bağlantı zaman aşımı
     */
    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(2);
    
    /**
     * Komut zaman aşımı
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(2);
    
    /**
     * Önbellek TTL süresi (saat)
     */
    private long cacheTtlHours = 24;
    
    /**
     * Bağlantı havuzu maksimum aktif bağlantı
     */
    @Builder.Default
    private int poolMaxActive = 32;
    
    /**
     * Bağlantı havuzu maksimum boşta bağlantı
     */
    @Builder.Default
    private int poolMaxIdle = 16;
    
    /**
     * Bağlantı havuzu minimum boşta bağlantı
     */
    @Builder.Default
    private int poolMinIdle = 8;
    
    /**
     * Bağlantı havuzu maksimum bekleme süresi
     */
    @Builder.Default
    private Duration poolMaxWait = Duration.ofMillis(-1);
    
    /**
     * Devre kesici etkin
     */
    private boolean circuitBreakerEnabled = true;
    
    /**
     * Devre kesici başarısızlık eşiği yüzdesi
     */
    @Builder.Default
    private float circuitBreakerFailureRateThreshold = 50.0f;
    
    /**
     * Devre kesici açık durumda bekleme süresi
     */
    @Builder.Default
    private Duration circuitBreakerWaitDurationInOpenState = Duration.ofSeconds(10);
    
    /**
     * Devre kesici yarı açık durumda izin verilen çağrı sayısı
     */
    @Builder.Default
    private int circuitBreakerPermittedCallsInHalfOpenState = 3;
    
    /**
     * Devre kesici izleme penceresi türü
     */
    @Builder.Default
    private String circuitBreakerSlidingWindowType = "COUNT_BASED";
    
    /**
     * Devre kesici izleme penceresi boyutu
     */
    @Builder.Default
    private int circuitBreakerSlidingWindowSize = 10;
    
    /**
     * Devre kesici minimum çağrı sayısı
     */
    @Builder.Default
    private int circuitBreakerMinimumNumberOfCalls = 3;
    
    /**
     * Retry etkin
     */
    private boolean retryEnabled = true;
    
    /**
     * Maksimum retry sayısı
     */
    private int maxRetryAttempts = 2;
    
    /**
     * Retry arasındaki bekleme süresi
     */
    private Duration retryWaitDuration = Duration.ofMillis(500);
}
