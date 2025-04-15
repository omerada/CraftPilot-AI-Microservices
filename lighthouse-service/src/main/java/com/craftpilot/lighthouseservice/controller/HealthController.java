package com.craftpilot.lighthouseservice.controller;

import lombok.RequiredArgsConstructor;
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
            boolean redisConnected = redisConnectionFactory.getConnection().ping() != null;
            Map<String, Object> status = Map.of(
                "status", redisConnected ? "UP" : "DOWN",
                "redis", redisConnected ? "UP" : "DOWN",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
        });
    }
}
