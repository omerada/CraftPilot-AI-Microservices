package com.craftpilot.lighthouseservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class HealthController {
    
    private final ApplicationAvailability applicationAvailability;
    private final ApplicationContext applicationContext;
    private ReactiveRedisConnectionFactory redisConnectionFactory;
    
    public HealthController(ApplicationAvailability applicationAvailability, ApplicationContext applicationContext) {
        this.applicationAvailability = applicationAvailability;
        this.applicationContext = applicationContext;
        
        try {
            this.redisConnectionFactory = applicationContext.getBean(ReactiveRedisConnectionFactory.class);
        } catch (Exception e) {
            log.warn("Redis connection factory not available: {}", e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        
        // Liveness check
        LivenessState livenessState = applicationAvailability.getLivenessState();
        healthStatus.put("liveness", livenessState.toString());
        
        // Readiness check
        ReadinessState readinessState = applicationAvailability.getReadinessState();
        healthStatus.put("readiness", readinessState.toString());
        
        // JVM ve sistem bilgilerini ekle
        healthStatus.put("jvm", getJvmInfo());
        
        // Redis bağlantı kontrolü
        return checkRedisConnection()
            .doOnNext(redisStatus -> healthStatus.put("redis", redisStatus))
            .thenReturn(new ResponseEntity<>(healthStatus, HttpStatus.OK))
            .onErrorResume(e -> {
                log.error("Health check error: {}", e.getMessage());
                healthStatus.put("status", "DOWN");
                healthStatus.put("error", e.getMessage());
                return Mono.just(new ResponseEntity<>(healthStatus, HttpStatus.SERVICE_UNAVAILABLE));
            });
    }
    
    private Map<String, Object> getJvmInfo() {
        Map<String, Object> jvmInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long allocatedMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));
        jvmInfo.put("max_memory_mb", maxMemory);
        jvmInfo.put("allocated_memory_mb", allocatedMemory);
        jvmInfo.put("free_memory_mb", freeMemory);
        jvmInfo.put("available_processors", runtime.availableProcessors());
        
        // JVM çalışma süresi
        try {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            jvmInfo.put("uptime_ms", uptimeMs);
        } catch (Exception e) {
            log.warn("Could not get JVM info: {}", e.getMessage());
            jvmInfo.put("uptime_ms", -1);
        }
        
        return jvmInfo;
    }
    
    private Mono<Map<String, Object>> checkRedisConnection() {
        if (redisConnectionFactory == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UNKNOWN");
            result.put("message", "Redis connection factory not available");
            return Mono.just(result);
        }
        
        return redisConnectionFactory.getReactiveConnection().ping()
            .timeout(Duration.ofSeconds(2))
            .map(pong -> {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "UP");
                result.put("ping", pong);
                return result;
            })
            .onErrorResume(e -> {
                log.error("Redis connection error: {}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("status", "DOWN");
                result.put("error", e.getMessage());
                return Mono.just(result);
            });
    }
    
    @GetMapping("/")
    public Mono<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "Lighthouse Analysis Service");
        response.put("status", "UP");
        return Mono.just(response);
    }
}
