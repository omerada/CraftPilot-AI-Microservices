
package com.craftpilot.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("UP");
    }
}