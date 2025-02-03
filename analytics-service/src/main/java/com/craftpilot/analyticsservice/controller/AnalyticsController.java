package com.craftpilot.analyticsservice.controller;

import com.craftpilot.analyticsservice.model.AnalyticsReport;
import com.craftpilot.analyticsservice.model.PerformanceMetrics;
import com.craftpilot.analyticsservice.model.UsageMetrics;
import com.craftpilot.analyticsservice.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics management APIs")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    // Usage Metrics Endpoints
    @PostMapping("/usage")
    @Operation(summary = "Record usage metrics")
    public Mono<ResponseEntity<UsageMetrics>> recordUsageMetrics(@RequestBody UsageMetrics metrics) {
        return analyticsService.recordUsageMetrics(metrics)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/usage/user/{userId}")
    @Operation(summary = "Get usage metrics by user ID")
    public Flux<UsageMetrics> getUserMetrics(@PathVariable String userId) {
        return analyticsService.getUserMetrics(userId);
    }

    // Performance Metrics Endpoints
    @PostMapping("/performance")
    @Operation(summary = "Record performance metrics")
    public Mono<ResponseEntity<PerformanceMetrics>> recordPerformanceMetrics(
            @RequestBody PerformanceMetrics metrics) {
        return analyticsService.recordPerformanceMetrics(metrics)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/performance/model/{modelId}")
    @Operation(summary = "Get performance metrics by model ID")
    public Flux<PerformanceMetrics> getModelPerformance(@PathVariable String modelId) {
        return analyticsService.getModelPerformance(modelId);
    }

    // Report Generation Endpoints
    @PostMapping("/reports")
    @Operation(summary = "Generate analytics report")
    public Mono<ResponseEntity<AnalyticsReport>> generateReport(@RequestBody AnalyticsReport report) {
        return analyticsService.generateReport(report)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/reports/type/{type}")
    @Operation(summary = "Get reports by type")
    public Flux<AnalyticsReport> getReportsByType(@PathVariable AnalyticsReport.ReportType type) {
        return analyticsService.getReportsByType(type);
    }

    @GetMapping("/reports/user/{userId}")
    @Operation(summary = "Get reports by user ID")
    public Flux<AnalyticsReport> getReportsByUser(@PathVariable String userId) {
        return analyticsService.getReportsByUser(userId);
    }
} 