package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.craftpilot.adminservice.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-metrics")
@RequiredArgsConstructor
@Tag(name = "System Metrics", description = "System metrics management APIs")
public class SystemMetricsController {
    private final SystemMetricsService systemMetricsService;

    @PostMapping
    @Operation(summary = "Save system metrics", description = "Save new system metrics data")
    public Mono<ResponseEntity<SystemMetrics>> saveMetrics(
            @RequestBody SystemMetrics metrics) {
        return systemMetricsService.saveMetrics(metrics)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get metrics by service ID", description = "Retrieve system metrics for a specific service")
    public Mono<ResponseEntity<SystemMetrics>> getMetricsById(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return systemMetricsService.getMetricsById(serviceId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all metrics", description = "Retrieve all system metrics")
    public Flux<SystemMetrics> getAllMetrics() {
        return systemMetricsService.getAllMetrics();
    }

    @GetMapping("/type/{serviceType}")
    @Operation(summary = "Get metrics by service type", description = "Retrieve system metrics for a specific service type")
    public Flux<SystemMetrics> getMetricsByServiceType(
            @Parameter(description = "Service type") @PathVariable SystemMetrics.ServiceType serviceType) {
        return systemMetricsService.getMetricsByServiceType(serviceType);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get metrics by status", description = "Retrieve system metrics with a specific status")
    public Flux<SystemMetrics> getMetricsByStatus(
            @Parameter(description = "Service status") @PathVariable SystemMetrics.ServiceStatus status) {
        return systemMetricsService.getMetricsByStatus(status);
    }

    @DeleteMapping("/{serviceId}")
    @Operation(summary = "Delete metrics", description = "Delete system metrics for a specific service")
    public Mono<ResponseEntity<Void>> deleteMetrics(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return systemMetricsService.deleteMetrics(serviceId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/unhealthy")
    @Operation(summary = "Get unhealthy services", description = "Retrieve metrics for all unhealthy services")
    public Mono<ResponseEntity<List<SystemMetrics>>> getUnhealthyServices() {
        return systemMetricsService.getUnhealthyServices()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{serviceId}/health")
    @Operation(summary = "Check service health", description = "Check if a specific service is healthy")
    public Mono<ResponseEntity<Boolean>> isServiceHealthy(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return systemMetricsService.isServiceHealthy(serviceId)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{serviceId}/status")
    @Operation(summary = "Update service status", description = "Update the status of a specific service")
    public Mono<ResponseEntity<SystemMetrics>> updateServiceStatus(
            @Parameter(description = "Service ID") @PathVariable String serviceId,
            @Parameter(description = "New status") @RequestParam SystemMetrics.ServiceStatus newStatus) {
        return systemMetricsService.updateServiceStatus(serviceId, newStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/maintenance")
    @Operation(summary = "Get services in maintenance", description = "Retrieve metrics for all services requiring maintenance")
    public Mono<ResponseEntity<List<SystemMetrics>>> getServicesRequiringMaintenance() {
        return systemMetricsService.getServicesRequiringMaintenance()
                .map(ResponseEntity::ok);
    }
} 