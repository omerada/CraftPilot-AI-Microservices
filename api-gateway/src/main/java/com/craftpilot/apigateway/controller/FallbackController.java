package com.craftpilot.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private Mono<ResponseEntity<Map<String, String>>> createFallbackResponse(String serviceName) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", serviceName + " service is currently unavailable");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    // Core Services
    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, String>>> userServiceFallback() {
        return createFallbackResponse("User");
    }

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, String>>> authServiceFallback() {
        return createFallbackResponse("Authentication");
    }

    // Business Services
    @GetMapping("/admin")
    public Mono<ResponseEntity<Map<String, String>>> adminServiceFallback() {
        return createFallbackResponse("Admin");
    }

    @GetMapping("/analytics")
    public Mono<ResponseEntity<Map<String, String>>> analyticsServiceFallback() {
        return createFallbackResponse("Analytics");
    }

    @GetMapping("/ai")
    public Mono<ResponseEntity<Map<String, String>>> llmServiceFallback() {
        return createFallbackResponse("Llm");
    }

    @GetMapping("/images")
    public Mono<ResponseEntity<Map<String, String>>> imageServiceFallback() {
        return createFallbackResponse("Image");
    }

    @GetMapping("/credits")
    public Mono<ResponseEntity<Map<String, String>>> creditServiceFallback() {
        return createFallbackResponse("Credit");
    }

    @GetMapping("/subscriptions")
    public Mono<ResponseEntity<Map<String, String>>> subscriptionServiceFallback() {
        return createFallbackResponse("Subscription");
    }

    @GetMapping("/notifications")
    public Mono<ResponseEntity<Map<String, String>>> notificationServiceFallback() {
        return createFallbackResponse("Notification");
    }

    @GetMapping("/cache")
    public Mono<ResponseEntity<Map<String, String>>> redisServiceFallback() {
        return createFallbackResponse("Cache/Redis");
    }
 
    @GetMapping("/subscription-plans")
    public Mono<ResponseEntity<Map<String, String>>> subscriptionPlansServiceFallback() {
        return createFallbackResponse("Subscription Plans");
    }

    @GetMapping("/payments")
    public Mono<ResponseEntity<Map<String, String>>> paymentsServiceFallback() {
        return createFallbackResponse("Payment");
    }
}