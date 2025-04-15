package com.craftpilot.lighthouseservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.fromCallable(() -> {
            boolean redisConnected = false;
            String errorMessage = null;
            
            try {
                redisConnected = redisConnectionFactory.getConnection().ping() != null;
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
            
            Map<String, Object> details = errorMessage != null ? 
                Map.of("connection", "error", "error", errorMessage) : 
                Map.of("connection", redisConnected ? "successful" : "failed");
            
            Map<String, Object> status = Map.of(
                "status", redisConnected ? "UP" : "DOWN",
                "redis", redisConnected ? "UP" : "DOWN",
                "timestamp", System.currentTimeMillis(),
                "details", details
            );
            
            return ResponseEntity.ok(status);
        });
    }
}
