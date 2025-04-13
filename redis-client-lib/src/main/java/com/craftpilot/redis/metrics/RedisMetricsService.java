package com.craftpilot.redis.metrics;

import com.craftpilot.redis.service.ReactiveRedisService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RedisMetricsService {

    private final MeterRegistry meterRegistry;
    private final ReactiveRedisService redisService;
    
    private Timer redisGetTimer;
    private Timer redisSetTimer;
    private Timer redisDeleteTimer;
    private AtomicInteger redisConnectionStatus;

    public RedisMetricsService(MeterRegistry meterRegistry, ReactiveRedisService redisService) {
        this.meterRegistry = meterRegistry;
        this.redisService = redisService;
    }

    @PostConstruct
    public void init() {
        // Timer'ları oluştur
        this.redisGetTimer = Timer.builder("redis.operation.get")
                .description("Redis get operasyonları için zaman ölçümü")
                .register(meterRegistry);
                
        this.redisSetTimer = Timer.builder("redis.operation.set")
                .description("Redis set operasyonları için zaman ölçümü")
                .register(meterRegistry);
                
        this.redisDeleteTimer = Timer.builder("redis.operation.delete")
                .description("Redis delete operasyonları için zaman ölçümü")
                .register(meterRegistry);
                
        // Redis bağlantı durumu için gauge
        this.redisConnectionStatus = meterRegistry.gauge(
                "redis.connection.status", 
                new AtomicInteger(redisService.isRedisHealthy() ? 1 : 0));
                
        // Düzenli olarak sağlık durumunu güncelle
        startHealthMonitoring();
    }
    
    /**
     * Redis sağlık durumunu düzenli olarak kontrol eder ve metrikleri günceller
     */
    private void startHealthMonitoring() {
        Thread monitoringThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    redisService.ping()
                        .subscribe(healthy -> redisConnectionStatus.set(healthy ? 1 : 0));
                    
                    // Her 30 saniyede bir kontrol et
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Redis sağlık izleme hatası: {}", e.getMessage());
                }
            }
        });
        
        monitoringThread.setName("redis-health-monitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }
    
    /**
     * Get operasyonu için timer'ı döndürür
     */
    public Timer getRedisGetTimer() {
        return redisGetTimer;
    }
    
    /**
     * Set operasyonu için timer'ı döndürür
     */
    public Timer getRedisSetTimer() {
        return redisSetTimer;
    }
    
    /**
     * Delete operasyonu için timer'ı döndürür
     */
    public Timer getRedisDeleteTimer() {
        return redisDeleteTimer;
    }
}
