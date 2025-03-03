package com.craftpilot.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{serviceName}")
    public Mono<ResponseEntity<Map<String, String>>> serviceFallback(
            @PathVariable String serviceName) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", formatServiceName(serviceName) + " service is currently unavailable");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "User service is temporarily unavailable");
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    private String formatServiceName(String serviceName) {
        return serviceName.substring(0, 1).toUpperCase() + 
               serviceName.substring(1).toLowerCase().replace("-", " ");
    }
}