package com.craftpilot.lighthouseservice.controller;

import com.craftpilot.lighthouseservice.model.AnalysisRequest;
import com.craftpilot.lighthouseservice.model.JobStatusResponse;
import com.craftpilot.lighthouseservice.service.LighthouseQueueService;
import com.craftpilot.lighthouseservice.service.LighthouseWorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lighthouse", description = "Lighthouse performance analysis API")
public class LighthouseController {
    private final LighthouseQueueService lighthouseQueueService;
    private final LighthouseWorkerService lighthouseWorkerService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze website performance", description = "Queue a new Lighthouse analysis job")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeWebsite(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("Received analysis request for URL: {}", request.getUrl());
        
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            log.warn("Received invalid request: URL is missing");
            return Mono.just(ResponseEntity.badRequest().body(
                Map.of("error", "URL is required", "status", "ERROR")
            ));
        }
        
        // Önce kuyruk durumunu kontrol et
        return lighthouseQueueService.getQueueLength()
            .flatMap(queueLength -> {
                log.info("Current queue length: {}", queueLength);
                
                // Kuyruk çok uzunsa hızlı bir şekilde yanıt ver
                if (queueLength > 20) { // Örnek sınır değer
                    log.warn("Queue is too long: {} jobs pending", queueLength);
                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                        Map.of(
                            "error", "Queue is full, please try again later",
                            "queueLength", queueLength,
                            "status", "QUEUE_FULL"
                        )
                    ));
                }
                
                log.info("Starting analysis for URL: {}", request.getUrl());
                return lighthouseQueueService.queueAnalysisJob(request.getUrl(), request.getOptions())
                    .map(jobId -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("jobId", jobId);
                        response.put("status", "PENDING");
                        response.put("url", request.getUrl());
                        response.put("queuePosition", queueLength + 1);
                        response.put("estimatedWaitTime", (queueLength + 1) * 15); // Tahmini 15 saniye/job
                        
                        // Worker'ları kontrol et ve gerekirse yeni görevleri işlemeye başla
                        lighthouseWorkerService.checkAndProcessQueue();
                        
                        return ResponseEntity.accepted().body(response);
                    });
            });
    }

    @GetMapping("/report/{jobId}")
    @Operation(summary = "Get analysis report", description = "Get the status or result of a Lighthouse analysis job")
    public Mono<ResponseEntity<JobStatusResponse>> getAnalysisReport(@PathVariable String jobId) {
        log.info("Getting report for job ID: {}", jobId);
        
        if (jobId == null || jobId.isEmpty()) {
            log.warn("Received invalid job ID");
            return Mono.just(ResponseEntity.badRequest().body(
                JobStatusResponse.builder()
                    .status("ERROR")
                    .error("Job ID is required")
                    .build()
            ));
        }
        
        log.debug("Fetching job status from Redis");
        
        return lighthouseQueueService.getJobStatus(jobId)
                .map(result -> {
                    if (result.isComplete()) {
                        if ("FAILED".equals(result.getStatus())) {
                            log.warn("Job {} failed: {}", jobId, result.getError());
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                        }
                        
                        if ("COMPLETED".equals(result.getStatus())) {
                            log.info("Job complete, returning result for job ID: {}", jobId);
                            return ResponseEntity.ok(result);
                        }
                        
                        // Diğer tamamlanma durumları
                        return ResponseEntity.ok(result);
                    } else if ("NOT_FOUND".equals(result.getStatus())) {
                        log.warn("Job {} not found", jobId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
                    } else {
                        log.info("Job pending, returning status for job ID: {}", jobId);
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error getting report for job ID: {}", jobId, error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(JobStatusResponse.builder()
                                    .jobId(jobId)
                                    .status("ERROR")
                                    .error("Failed to get report: " + error.getMessage())
                                    .build()));
                });
    }
    
    @GetMapping("/queue-status")
    @Operation(summary = "Get queue status", description = "Get current queue length and worker status")
    public Mono<ResponseEntity<Map<String, Object>>> getQueueStatus() {
        return lighthouseQueueService.getQueueLength()
            .map(queueLength -> {
                Map<String, Object> status = new HashMap<>();
                status.put("queueLength", queueLength);
                status.put("activeWorkers", lighthouseWorkerService.getActiveWorkerCount());
                status.put("estimatedWaitTime", queueLength * 15); // Tahmini 15 saniye/job
                
                return ResponseEntity.ok(status);
            });
    }
}
