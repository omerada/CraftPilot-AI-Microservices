package com.craftpilot.lighthouseservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class HealthController {

    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final ApplicationAvailability applicationAvailability;

    @Autowired
    public HealthController(ReactiveRedisConnectionFactory redisConnectionFactory, 
                           ApplicationAvailability applicationAvailability) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.applicationAvailability = applicationAvailability;
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return checkRedisConnection()
            .map(redisStatus -> {
                Map<String, Object> health = new HashMap<>();
                
                // Application state
                LivenessState livenessState = applicationAvailability.getLivenessState();
                ReadinessState readinessState = applicationAvailability.getReadinessState();
                
                boolean isSystemUp = readinessState == ReadinessState.ACCEPTING_TRAFFIC && 
                                     livenessState == LivenessState.CORRECT;
                                     
                String overallStatus = isSystemUp && redisStatus ? "UP" : "DOWN";
                
                health.put("status", overallStatus);
                health.put("application", isSystemUp ? "UP" : "DOWN");
                health.put("redis", redisStatus ? "UP" : "DOWN");
                health.put("timestamp", System.currentTimeMillis());
                
                Map<String, Object> details = new HashMap<>();
                details.put("liveness", livenessState.toString());
                details.put("readiness", readinessState.toString());
                details.put("connection", redisStatus ? "successful" : "failed");
                health.put("details", details);
                
                HttpStatus status = "UP".equals(overallStatus) ? 
                    HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
                
                return new ResponseEntity<>(health, status);
            })
            .onErrorResume(e -> {
                log.warn("Health check failed", e);
                
                Map<String, Object> health = new HashMap<>();
                health.put("status", "DOWN");
                health.put("application", "UP");
                health.put("redis", "DOWN");
                health.put("timestamp", System.currentTimeMillis());
                
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getClass().getSimpleName());
                details.put("message", e.getMessage());
                health.put("details", details);
                
                return Mono.just(new ResponseEntity<>(health, HttpStatus.SERVICE_UNAVAILABLE));
            });
    }

    private Mono<Boolean> checkRedisConnection() {
        return Mono.defer(() -> {
            try {
                return redisConnectionFactory.getReactiveConnection()
                    .ping()
                    .map(ping -> true)
                    .timeout(Duration.ofMillis(1000))
                    .onErrorResume(e -> {
                        log.warn("Redis connection check failed", e);
                        return Mono.just(false);
                    });
            } catch (Exception e) {
                log.warn("Redis connection check failed", e);
                return Mono.just(false);
            }
        });
    }
}
