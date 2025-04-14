package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.performance.*;
import com.craftpilot.llmservice.service.PerformanceService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Performance", description = "Web site performance optimization APIs")
public class PerformanceController {
    private final PerformanceService performanceService;
    
    // IP başına rate limiting için bucket'lar
    private final Map<String, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();
    
    private Bucket createBucket() {
        // IP başına dakikada 5 istek limiti
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
    
    private Mono<Boolean> checkRateLimit(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        Bucket bucket = rateLimitBuckets.computeIfAbsent(ip, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            return Mono.just(true);
        } else {
            return Mono.just(false);
        }
    }
    
    @PostMapping("/analyze")
    @Operation(summary = "Web sitesi performans analizi yap")
    public Mono<ResponseEntity<PerformanceAnalysisResponse>> analyzeWebsite(
            @Valid @RequestBody PerformanceAnalysisRequest request,
            ServerWebExchange exchange
    ) {
        return checkRateLimit(exchange)
                .flatMap(allowed -> {
                    if (!allowed) {
                        log.warn("Rate limit exceeded for IP: {}", exchange.getRequest().getRemoteAddress());
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
                    }
                    
                    log.info("Performance analysis request received for URL: {}", request.getUrl());
                    
                    return performanceService.analyzeWebsite(request)
                            .map(ResponseEntity::ok)
                            .doOnSuccess(response -> log.info("Performance analysis completed for URL: {}", request.getUrl()))
                            .onErrorResume(e -> {
                                log.error("Error during performance analysis", e);
                                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(PerformanceAnalysisResponse.builder()
                                                .build()));
                            });
                });
    }
    
    @PostMapping("/suggestions")
    @Operation(summary = "Generate performance improvement suggestions")
    public Mono<SuggestionsResponse> generateSuggestions(@RequestBody SuggestionsRequest request) {
        log.info("Generating performance suggestions for request");
        return performanceService.generateSuggestions(request);
    }
    
    @GetMapping(value = "/suggestions/stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    @Operation(summary = "Stream performance improvement suggestions")
    public Flux<StreamSuggestionsResponse> streamSuggestions(
            @RequestParam String url,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer maxTokens,
            @RequestParam(required = false) Double temperature,
            @RequestParam(required = false, defaultValue = "tr") String language) {
        
        log.info("Streaming performance suggestions for URL: {}", url);
        
        StreamSuggestionsRequest request = StreamSuggestionsRequest.builder()
                .url(url)
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .model(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .language(language)
                .build();
                
        return performanceService.streamSuggestions(request);
    }
    
    @PostMapping("/history")
    @Operation(summary = "Web sitesi performans analiz geçmişini getir")
    public Mono<ResponseEntity<PerformanceHistoryResponse>> getPerformanceHistory(
            @Valid @RequestBody PerformanceHistoryRequest request,
            ServerWebExchange exchange
    ) {
        return checkRateLimit(exchange)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
                    }
                    
                    log.info("Performance history request received for URL: {}", request.getUrl());
                    
                    return performanceService.getPerformanceHistory(request)
                            .map(ResponseEntity::ok)
                            .doOnSuccess(response -> log.info("Performance history retrieved for URL: {}", request.getUrl()))
                            .onErrorResume(e -> {
                                log.error("Error retrieving performance history", e);
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                            });
                });
    }
}
