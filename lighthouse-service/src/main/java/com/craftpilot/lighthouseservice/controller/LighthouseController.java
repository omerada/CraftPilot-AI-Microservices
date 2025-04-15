package com.craftpilot.lighthouseservice.controller;

import com.craftpilot.lighthouseservice.model.AnalysisRequest;
import com.craftpilot.lighthouseservice.model.JobStatusResponse;
import com.craftpilot.lighthouseservice.service.LighthouseQueueService;
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

    @PostMapping("/analyze")
    @Operation(summary = "Analyze website performance", description = "Queue a new Lighthouse analysis job")
    public Mono<ResponseEntity<Map<String, String>>> analyzeWebsite(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("Received analysis request for URL: {}", request.getUrl());
        
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            log.warn("Received invalid request: URL is missing");
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "URL is required")));
        }
        
        log.info("Starting analysis for URL: {}", request.getUrl());
        return lighthouseQueueService.queueAnalysisJob(request.getUrl(), request.getOptions())
                .map(jobId -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("jobId", jobId);
                    return ResponseEntity.accepted().body(response);
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
                        log.info("Job complete, returning result for job ID: {}", jobId);
                        return ResponseEntity.ok(result);
                    } else {
                        log.info("Job pending, returning status for job ID: {}", jobId);
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error getting report for job ID: {}", jobId, error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(JobStatusResponse.builder()
                                    .status("ERROR")
                                    .error("Failed to get report: " + error.getMessage())
                                    .build()));
                });
    }
}
