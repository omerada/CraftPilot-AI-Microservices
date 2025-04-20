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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // Manual logger for fallback
    private static final Logger logger = LoggerFactory.getLogger(LighthouseController.class);
    
    private final LighthouseQueueService lighthouseQueueService;
    private final LighthouseWorkerService lighthouseWorkerService;

    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze website performance", 
        description = "Queue a new Lighthouse analysis job. You can specify analysisType as 'basic' or 'detailed' and deviceType as 'mobile' or 'desktop'."
    )
    public Mono<ResponseEntity<Map<String, Object>>> analyzeWebsite(
            @Valid @RequestBody AnalysisRequest request) {
        logger.info("Received analysis request for URL: {} with type: {} for device: {}", 
                    request.getUrl(), request.getAnalysisType(), request.getDeviceType());
        
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            logger.warn("Empty URL received");
            return Mono.just(ResponseEntity.badRequest().body(
                Map.of(
                    "error", "URL cannot be empty",
                    "status", "ERROR"
                )
            ));
        }
        
        String analysisType = request.getAnalysisType();
        String deviceType = request.getDeviceType();
        
        if (analysisType != null && !analysisType.equals("basic") && !analysisType.equals("detailed")) {
            logger.warn("Invalid analysisType: {}", analysisType);
            return Mono.just(ResponseEntity.badRequest().body(
                Map.of(
                    "error", "analysisType must be either 'basic' or 'detailed'",
                    "status", "ERROR"
                )
            ));
        }
        
        if (deviceType != null && !deviceType.equals("desktop") && !deviceType.equals("mobile")) {
            logger.warn("Invalid deviceType: {}", deviceType);
            return Mono.just(ResponseEntity.badRequest().body(
                Map.of(
                    "error", "deviceType must be either 'desktop' or 'mobile'",
                    "status", "ERROR"
                )
            ));
        }

        // Önce kuyruk durumunu kontrol et
        return lighthouseQueueService.getQueueLength()
            .flatMap(queueLength -> {
                logger.info("Current queue length: {}", queueLength);
                
                // Kuyruk çok uzunsa hızlı bir şekilde yanıt ver
                if (queueLength > 20) { // Örnek sınır değer
                    logger.warn("Queue is too long: {} jobs pending", queueLength);
                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                        Map.of(
                            "error", "Queue is full, please try again later",
                            "queueLength", queueLength,
                            "status", "QUEUE_FULL"
                        )
                    ));
                }
                
                logger.info("Starting analysis for URL: {} with type: {} for device: {}", 
                           request.getUrl(), analysisType, deviceType);
                
                // Options nesnesini hazırla
                Map<String, Object> options = request.getOptions() != null ? 
                    new HashMap<>(request.getOptions()) : new HashMap<>();
                
                // AnalysisType ve deviceType'ı options'a ekle
                options.put("analysisType", analysisType);
                options.put("deviceType", deviceType);
                
                return lighthouseQueueService.queueAnalysisJob(request.getUrl(), options)
                    .map(jobId -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("jobId", jobId);
                        response.put("status", "PENDING");
                        response.put("url", request.getUrl());
                        response.put("analysisType", analysisType);
                        response.put("deviceType", deviceType);
                        response.put("queuePosition", queueLength + 1);
                        
                        // İş kuyruğa alındı, worker'lar zaten otomatik olarak işleri kontrol ediyor
                        
                        return ResponseEntity.accepted().body(response);
                    });
            });
    }

    @GetMapping("/report/{jobId}")
    @Operation(summary = "Get analysis report", description = "Get the status or result of a Lighthouse analysis job")
    public Mono<ResponseEntity<JobStatusResponse>> getAnalysisReport(@PathVariable String jobId) {
        logger.info("Getting report for job ID: {}", jobId);
        
        if (jobId == null || jobId.isEmpty()) {
            logger.warn("Received invalid job ID");
            JobStatusResponse errorResponse = new JobStatusResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setError("Job ID is required");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
        
        logger.debug("Fetching job status from Redis");
        
        return lighthouseQueueService.getJobStatus(jobId)
                .map(result -> {
                    if (result.isComplete()) {
                        if ("FAILED".equals(result.getStatus())) {
                            logger.warn("Job {} failed: {}", jobId, result.getError());
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                        }
                        
                        if ("COMPLETED".equals(result.getStatus())) {
                            logger.info("Job complete, returning result for job ID: {}", jobId);
                            return ResponseEntity.ok(result);
                        }
                        
                        // Diğer tamamlanma durumları
                        return ResponseEntity.ok(result);
                    } else if ("NOT_FOUND".equals(result.getStatus())) {
                        logger.warn("Job {} not found", jobId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
                    } else if ("PROCESSING".equals(result.getStatus())) {
                        logger.info("Job is currently being processed by worker for job ID: {}", jobId);
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
                    } else {
                        logger.info("Job pending, waiting for worker pickup for job ID: {}", jobId); 
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error getting report for job ID: {}", jobId, error);
                    JobStatusResponse errorResponse = new JobStatusResponse();
                    errorResponse.setJobId(jobId);
                    errorResponse.setStatus("ERROR");
                    errorResponse.setComplete(false);
                    errorResponse.setError("Failed to get report: " + error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
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
                
                // Tahmini süre kaldırıldı
                
                return ResponseEntity.ok(status);
            });
    }
}
