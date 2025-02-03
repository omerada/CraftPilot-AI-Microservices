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

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, String>>> userServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "User service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/admin")
    public Mono<ResponseEntity<Map<String, String>>> adminServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Admin service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/question")
    public Mono<ResponseEntity<Map<String, String>>> questionServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Question service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/chat")
    public Mono<ResponseEntity<Map<String, String>>> chatServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Chat service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/image")
    public Mono<ResponseEntity<Map<String, String>>> imageServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Image service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/code")
    public Mono<ResponseEntity<Map<String, String>>> codeServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Code service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/translation")
    public Mono<ResponseEntity<Map<String, String>>> translationServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Translation service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/content")
    public Mono<ResponseEntity<Map<String, String>>> contentServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Content service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/model")
    public Mono<ResponseEntity<Map<String, String>>> modelServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Model service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/analytics")
    public Mono<ResponseEntity<Map<String, String>>> analyticsServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Analytics service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/notification")
    public Mono<ResponseEntity<Map<String, String>>> notificationServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Notification service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/credit")
    public Mono<ResponseEntity<Map<String, String>>> creditServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Credit service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/subscription")
    public Mono<ResponseEntity<Map<String, String>>> subscriptionServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Subscription service is currently unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
} 