package com.craftpilot.activitylogservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class HealthController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        log.info("Custom health endpoint called");
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "activity-log-service");
        health.put("timestamp", System.currentTimeMillis());
        return Mono.just(health);
    }
}
