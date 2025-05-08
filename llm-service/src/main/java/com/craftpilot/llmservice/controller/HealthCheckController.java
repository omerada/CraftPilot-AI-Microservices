package com.craftpilot.llmservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class HealthCheckController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        log.info("Health check endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "llm-service");
        
        // JVM bilgilerini ekle
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        response.put("uptime", runtimeBean.getUptime());
        response.put("startTime", runtimeBean.getStartTime());
        
        return Mono.just(response);
    }
    
    @GetMapping("/")
    public Mono<String> root() {
        log.info("Root endpoint called");
        return Mono.just("LLM Service is running");
    }
}
