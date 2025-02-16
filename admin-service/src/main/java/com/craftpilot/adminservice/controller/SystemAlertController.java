package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.SystemAlert;
import com.craftpilot.adminservice.service.SystemAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/system-alerts")
@RequiredArgsConstructor
@Tag(name = "System Alerts", description = "System alert management APIs")
public class SystemAlertController {
    private final SystemAlertService systemAlertService;

    @PostMapping
    @Operation(summary = "Create alert", description = "Create a new system alert")
    public Mono<ResponseEntity<SystemAlert>> createAlert(
            @RequestBody SystemAlert alert) {
        return systemAlertService.createAlert(alert)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID", description = "Retrieve a specific system alert")
    public Mono<ResponseEntity<SystemAlert>> getAlertById(
            @Parameter(description = "Alert ID") @PathVariable String id) {
        return systemAlertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get service alerts", description = "Retrieve all alerts for a specific service")
    public Flux<SystemAlert> getAlertsByService(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return systemAlertService.getAlertsByService(serviceId);
    }

    @GetMapping("/type/{alertType}")
    @Operation(summary = "Get alerts by type", description = "Retrieve alerts of a specific type")
    public Flux<SystemAlert> getAlertsByType(
            @Parameter(description = "Alert type") @PathVariable SystemAlert.AlertType alertType) {
        return systemAlertService.getAlertsByType(alertType);
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get alerts by severity", description = "Retrieve alerts with a specific severity level")
    public Flux<SystemAlert> getAlertsBySeverity(
            @Parameter(description = "Alert severity") @PathVariable SystemAlert.AlertSeverity severity) {
        return systemAlertService.getAlertsBySeverity(severity);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get alerts by status", description = "Retrieve alerts with a specific status")
    public Flux<SystemAlert> getAlertsByStatus(
            @Parameter(description = "Alert status") @PathVariable SystemAlert.AlertStatus status) {
        return systemAlertService.getAlertsByStatus(status);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get alerts by time range", description = "Retrieve alerts within a specific time range")
    public Flux<SystemAlert> getAlertsByTimeRange(
            @Parameter(description = "Start time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return systemAlertService.getAlertsByTimeRange(start, end);
    }

    @GetMapping("/assignee/{assignedTo}")
    @Operation(summary = "Get alerts by assignee", description = "Retrieve alerts assigned to a specific user")
    public Flux<SystemAlert> getAlertsByAssignee(
            @Parameter(description = "Assignee ID") @PathVariable String assignedTo) {
        return systemAlertService.getAlertsByAssignee(assignedTo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert", description = "Delete a specific system alert")
    public Mono<ResponseEntity<Void>> deleteAlert(
            @Parameter(description = "Alert ID") @PathVariable String id) {
        return systemAlertService.deleteAlert(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update alert status", description = "Update the status of a specific alert")
    public Mono<ResponseEntity<SystemAlert>> updateAlertStatus(
            @Parameter(description = "Alert ID") @PathVariable String id,
            @Parameter(description = "New status") @RequestParam SystemAlert.AlertStatus newStatus) {
        return systemAlertService.updateAlertStatus(id, newStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign alert", description = "Assign an alert to a specific user")
    public Mono<ResponseEntity<SystemAlert>> assignAlert(
            @Parameter(description = "Alert ID") @PathVariable String id,
            @Parameter(description = "Assignee ID") @RequestParam String assignedTo) {
        return systemAlertService.assignAlert(id, assignedTo)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active alerts", description = "Retrieve all active alerts")
    public Mono<ResponseEntity<List<SystemAlert>>> getActiveAlerts() {
        return systemAlertService.getActiveAlerts()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/critical")
    @Operation(summary = "Get critical alerts", description = "Retrieve all critical alerts")
    public Mono<ResponseEntity<List<SystemAlert>>> getCriticalAlerts() {
        return systemAlertService.getCriticalAlerts()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Get unassigned alerts", description = "Retrieve all unassigned alerts")
    public Mono<ResponseEntity<List<SystemAlert>>> getUnassignedAlerts() {
        return systemAlertService.getUnassignedAlerts()
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve alert", description = "Resolve a specific alert with resolution details")
    public Mono<ResponseEntity<SystemAlert>> resolveAlert(
            @Parameter(description = "Alert ID") @PathVariable String id,
            @Parameter(description = "Resolution details") @RequestParam String resolution) {
        return systemAlertService.resolveAlert(id, resolution)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/service/{serviceId}/critical-state")
    @Operation(summary = "Check critical state", description = "Check if a service is in a critical state")
    public Mono<ResponseEntity<Boolean>> isServiceInCriticalState(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return systemAlertService.isServiceInCriticalState(serviceId)
                .map(ResponseEntity::ok);
    }
} 