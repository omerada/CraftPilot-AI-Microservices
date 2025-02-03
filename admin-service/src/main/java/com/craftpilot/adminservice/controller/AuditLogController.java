package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.AuditLog;
import com.craftpilot.adminservice.service.AuditLogService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log management APIs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @PostMapping
    @Operation(summary = "Log activity", description = "Record a new audit log entry")
    public Mono<ResponseEntity<AuditLog>> logActivity(
            @RequestBody AuditLog auditLog) {
        return auditLogService.logActivity(auditLog)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get log by ID", description = "Retrieve a specific audit log entry")
    public Mono<ResponseEntity<AuditLog>> getLogById(
            @Parameter(description = "Log ID") @PathVariable String id) {
        return auditLogService.getLogById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user logs", description = "Retrieve all audit logs for a specific user")
    public Flux<AuditLog> getLogsByUser(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return auditLogService.getLogsByUser(userId);
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get service logs", description = "Retrieve all audit logs for a specific service")
    public Flux<AuditLog> getLogsByService(
            @Parameter(description = "Service ID") @PathVariable String serviceId) {
        return auditLogService.getLogsByService(serviceId);
    }

    @GetMapping("/type/{logType}")
    @Operation(summary = "Get logs by type", description = "Retrieve audit logs of a specific type")
    public Flux<AuditLog> getLogsByType(
            @Parameter(description = "Log type") @PathVariable AuditLog.LogType logType) {
        return auditLogService.getLogsByType(logType);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get logs by status", description = "Retrieve audit logs with a specific status")
    public Flux<AuditLog> getLogsByStatus(
            @Parameter(description = "Log status") @PathVariable AuditLog.LogStatus status) {
        return auditLogService.getLogsByStatus(status);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get logs by time range", description = "Retrieve audit logs within a specific time range")
    public Flux<AuditLog> getLogsByTimeRange(
            @Parameter(description = "Start time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return auditLogService.getLogsByTimeRange(start, end);
    }

    @GetMapping("/resource/{resource}")
    @Operation(summary = "Get logs by resource", description = "Retrieve audit logs for a specific resource")
    public Flux<AuditLog> getLogsByResource(
            @Parameter(description = "Resource") @PathVariable String resource) {
        return auditLogService.getLogsByResource(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete log", description = "Delete a specific audit log entry")
    public Mono<ResponseEntity<Void>> deleteLog(
            @Parameter(description = "Log ID") @PathVariable String id) {
        return auditLogService.deleteLog(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/security")
    @Operation(summary = "Get security logs", description = "Retrieve all security-related audit logs")
    public Mono<ResponseEntity<List<AuditLog>>> getSecurityLogs() {
        return auditLogService.getSecurityLogs()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/errors")
    @Operation(summary = "Get error logs", description = "Retrieve all error-related audit logs")
    public Mono<ResponseEntity<List<AuditLog>>> getErrorLogs() {
        return auditLogService.getErrorLogs()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent logs", description = "Retrieve audit logs since a specific time")
    public Mono<ResponseEntity<List<AuditLog>>> getRecentLogs(
            @Parameter(description = "Since time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return auditLogService.getRecentLogs(since)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/security-event")
    @Operation(summary = "Log security event", description = "Record a new security-related audit log entry")
    public Mono<ResponseEntity<AuditLog>> logSecurityEvent(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Action") @RequestParam String action,
            @Parameter(description = "Resource") @RequestParam String resource,
            @Parameter(description = "Details") @RequestBody Map<String, Object> details) {
        return auditLogService.logSecurityEvent(userId, action, resource, details)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/operational-event")
    @Operation(summary = "Log operational event", description = "Record a new operational audit log entry")
    public Mono<ResponseEntity<AuditLog>> logOperationalEvent(
            @Parameter(description = "Service ID") @RequestParam String serviceId,
            @Parameter(description = "Action") @RequestParam String action,
            @Parameter(description = "Operation details") @RequestBody Map<String, Object> operationDetails) {
        return auditLogService.logOperationalEvent(serviceId, action, operationDetails)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/error-event")
    @Operation(summary = "Log error event", description = "Record a new error-related audit log entry")
    public Mono<ResponseEntity<AuditLog>> logErrorEvent(
            @Parameter(description = "Service ID") @RequestParam String serviceId,
            @Parameter(description = "Action") @RequestParam String action,
            @Parameter(description = "Error details") @RequestParam String errorDetails,
            @Parameter(description = "Context") @RequestBody Map<String, Object> context) {
        return auditLogService.logErrorEvent(serviceId, action, errorDetails, context)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/count/{userId}")
    @Operation(summary = "Get log count", description = "Get the total number of audit logs for a user")
    public Mono<ResponseEntity<Long>> getLogCount(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return auditLogService.getLogCount(userId)
                .map(ResponseEntity::ok);
    }
} 