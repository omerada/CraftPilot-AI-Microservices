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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * Performans analizi için dış dünya endpointlerini sağlar.
 * Bu controller, frontend isteklerini Lighthouse Service'e yönlendirmek için kullanılır.
 */
@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final PerformanceService performanceService;
    private final WebClient.Builder webClientBuilder;

    @Value("${lighthouse.service.url:http://lighthouse-service:8085}")
    private String lighthouseServiceUrl;

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

    /**
     * Web sitesi performans analizi başlatır
     * @param request Analiz isteği
     * @return Analiz sonucu veya durum bilgisi
     */
    @PostMapping("/analyze")
    public Mono<Map<String, Object>> analyzeWebsite(@RequestBody PerformanceAnalysisRequest request) {
        log.info("Performing performance analysis for URL: {} with type: {}", request.getUrl(), request.getAnalysisType());

        // Önce health check yap
        return healthCheck()
            .flatMap(isHealthy -> {
                if (!isHealthy) {
                    log.warn("Lighthouse service health check failed");
                    return Mono.<Map<String, Object>>just(Map.of(
                        "error", "Lighthouse service is currently unavailable",
                        "status", "SERVICE_UNAVAILABLE"
                    ));
                }
                
                // Servis sağlıklı, analiz isteğini gönder
                // analysisType parametresini de Lighthouse servisine aktar
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("url", request.getUrl());
                requestBody.put("analysisType", request.getAnalysisType());
                
                return webClientBuilder.build()
                    .post()
                    .uri(lighthouseServiceUrl + "/api/v1/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody) // Map olarak gönderip tüm parametreleri içerir
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
                    .doOnSuccess(response -> log.info("Performance analysis response received: jobId={}", response.get("jobId")))
                    .doOnError(error -> log.error("Error during performance analysis: {}", error.getMessage()));
            })
            .onErrorResume(error -> {
                log.error("Error analyzing website: {}", error.getMessage());
                return Mono.<Map<String, Object>>just(Map.of(
                    "error", "Failed to analyze website",
                    "message", error.getMessage(),
                    "status", "ERROR"
                ));
            });
    }

    @GetMapping("/status/{jobId}")
    @Operation(summary = "Check job status", description = "Check the status of a Lighthouse analysis job")
    public Mono<Map<String, Object>> checkStatus(@PathVariable String jobId) {
        log.info("Checking status for job: {}", jobId);
        
        return webClientBuilder.build()
            .get()
            .uri(lighthouseServiceUrl + "/api/v1/report/" + jobId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(response -> {
                Boolean complete = (Boolean) response.getOrDefault("complete", false);
                String status = (String) response.getOrDefault("status", "UNKNOWN");
                log.info("Status for job {}: complete={}, status={}", jobId, complete, status);
            })
            .doOnError(error -> log.error("Error checking job status: {}", error.getMessage()))
            .onErrorResume(error -> Mono.just(Map.of(
                "jobId", jobId,
                "status", "ERROR",
                "error", "Failed to check status: " + error.getMessage()
            )));
    }

    private Mono<Boolean> healthCheck() {
        return webClientBuilder.build()
            .get()
            .uri(lighthouseServiceUrl + "/health")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
            .map(response -> "UP".equals(response.get("status")))
            .onErrorReturn(false)
            .timeout(Duration.ofSeconds(2))
            .onErrorReturn(false);
    }

    /**
     * Belirli bir analiz işinin durumunu veya sonucunu getirir
     * @param jobId İş kimliği
     * @return İş durumu veya analiz sonuçları
     * 
     * Yanıt formatı:
     * 1. İşlem tamamlandıysa:
     * {
     *   "complete": true,
     *   "data": {
     *     "performance": 0.85,
     *     "audits": {
     *       "first-contentful-paint": { "score": 0.9, "displayValue": "1.2s", ... },
     *       "largest-contentful-paint": { "score": 0.8, "displayValue": "2.5s", ... },
     *       ...
     *     },
     *     "url": "https://example.com",
     *     "categories": {
     *       "performance": { "score": 0.85 },
     *       "accessibility": { "score": 0.92 },
     *       ...
     *     },
     *     ...
     *   }
     * }
     * 
     * 2. İşlem hala devam ediyorsa:
     * {
     *   "complete": false,
     *   "status": "PENDING",
     *   "jobId": "job-abc123",
     *   "message": "Analiz devam ediyor, lütfen daha sonra tekrar deneyin",
     *   "url": "https://example.com"
     * }
     * 
     * 3. Bir hata oluşmuşsa:
     * {
     *   "error": "Failed to retrieve report: 404",
     *   "message": "Job not found",
     *   "jobId": "job-abc123"
     * }
     */
    @GetMapping("/report/{jobId}")
    public Mono<Map<String, Object>> getAnalysisReport(@PathVariable String jobId) {
        log.info("Getting analysis report for jobId: {}", jobId);

        return webClientBuilder.build()
                .get()
                .uri(lighthouseServiceUrl + "/api/v1/report/" + jobId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
                .doOnSuccess(response -> {
                    Boolean complete = (Boolean) response.getOrDefault("complete", false);
                    log.info("Report status for jobId={}: complete={}", jobId, complete);
                })
                .doOnError(error -> log.error("Error fetching report: {}", error.getMessage()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Web client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Map.of(
                            "error", "Failed to retrieve report: " + e.getStatusCode(),
                            "message", e.getResponseBodyAsString(),
                            "jobId", jobId
                    ));
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "error", "Failed to retrieve report",
                        "message", e.getMessage(),
                        "jobId", jobId
                )));
    }
 
    @PostMapping("/suggestions")
    @Operation(summary = "Generate performance improvement suggestions")
    public Mono<SuggestionsResponse> generateSuggestions(@RequestBody SuggestionsRequest request) {
        log.info("Generating performance suggestions for request");
        return performanceService.generateSuggestions(request);
    }

    @GetMapping(value = "/performance/suggestions/stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
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
                                log.error("Error retrieving performance history: {}", e.getMessage(), e);

                                PerformanceHistoryResponse errorResponse = PerformanceHistoryResponse.builder()
                                        .url(request.getUrl())
                                        .error("Performans geçmişi getirilemedi: " + e.getMessage())
                                        .build();

                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(errorResponse));
                            });
                });
    }
}
