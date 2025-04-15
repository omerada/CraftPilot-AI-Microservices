package com.craftpilot.lighthouseservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            Map<String, Object> details = new HashMap<>();
            boolean redisConnected = false;
            String errorMessage = null;
            
            try {
                log.debug("Checking Redis connection...");
                redisConnected = redisConnectionFactory.getConnection().ping() != null;
                log.debug("Redis connection check result: {}", redisConnected);
            } catch (Exception e) {
                log.warn("Redis connection check failed", e);
                errorMessage = e.getMessage();
            }
            
            if (errorMessage != null) {
                details.put("connection", "error");
                details.put("error", errorMessage);
            } else {
                details.put("connection", redisConnected ? "successful" : "failed");
            }
            
            status.put("status", redisConnected ? "UP" : "DOWN");
            status.put("redis", redisConnected ? "UP" : "DOWN");
            status.put("timestamp", System.currentTimeMillis());
            status.put("details", details);
            
            return ResponseEntity.ok(status);
        });
    }
}
