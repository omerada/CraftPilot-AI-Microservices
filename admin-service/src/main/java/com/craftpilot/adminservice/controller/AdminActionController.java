package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.AdminAction;
import com.craftpilot.adminservice.service.AdminActionService;
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
@RequestMapping("/admin-actions")
@RequiredArgsConstructor
@Tag(name = "Admin Actions", description = "Admin action management APIs")
public class AdminActionController {
    private final AdminActionService adminActionService;

    @PostMapping
    @Operation(summary = "Record action", description = "Record a new admin action")
    public Mono<ResponseEntity<AdminAction>> recordAction(
            @RequestBody AdminAction action) {
        return adminActionService.recordAction(action)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get action by ID", description = "Retrieve a specific admin action")
    public Mono<ResponseEntity<AdminAction>> getActionById(
            @Parameter(description = "Action ID") @PathVariable String id) {
        return adminActionService.getActionById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/{adminId}")
    @Operation(summary = "Get admin actions", description = "Retrieve all actions for a specific admin")
    public Flux<AdminAction> getActionsByAdmin(
            @Parameter(description = "Admin ID") @PathVariable String adminId) {
        return adminActionService.getActionsByAdmin(adminId);
    }

    @GetMapping("/type/{actionType}")
    @Operation(summary = "Get actions by type", description = "Retrieve actions of a specific type")
    public Flux<AdminAction> getActionsByType(
            @Parameter(description = "Action type") @PathVariable AdminAction.ActionType actionType) {
        return adminActionService.getActionsByType(actionType);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get actions by status", description = "Retrieve actions with a specific status")
    public Flux<AdminAction> getActionsByStatus(
            @Parameter(description = "Action status") @PathVariable AdminAction.ActionStatus status) {
        return adminActionService.getActionsByStatus(status);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get actions by time range", description = "Retrieve actions within a specific time range")
    public Flux<AdminAction> getActionsByTimeRange(
            @Parameter(description = "Start time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return adminActionService.getActionsByTimeRange(start, end);
    }

    @GetMapping("/target/{targetId}")
    @Operation(summary = "Get actions by target", description = "Retrieve actions for a specific target")
    public Flux<AdminAction> getActionsByTarget(
            @Parameter(description = "Target ID") @PathVariable String targetId) {
        return adminActionService.getActionsByTarget(targetId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete action", description = "Delete a specific admin action")
    public Mono<ResponseEntity<Void>> deleteAction(
            @Parameter(description = "Action ID") @PathVariable String id) {
        return adminActionService.deleteAction(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update action status", description = "Update the status of a specific action")
    public Mono<ResponseEntity<AdminAction>> updateActionStatus(
            @Parameter(description = "Action ID") @PathVariable String id,
            @Parameter(description = "New status") @RequestParam AdminAction.ActionStatus newStatus) {
        return adminActionService.updateActionStatus(id, newStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending actions", description = "Retrieve all pending actions")
    public Mono<ResponseEntity<List<AdminAction>>> getPendingActions() {
        return adminActionService.getPendingActions()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent actions", description = "Retrieve actions since a specific time")
    public Mono<ResponseEntity<List<AdminAction>>> getRecentActions(
            @Parameter(description = "Since time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return adminActionService.getRecentActions(since)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/count/{adminId}")
    @Operation(summary = "Get action count", description = "Get the total number of actions for an admin")
    public Mono<ResponseEntity<Long>> getActionCount(
            @Parameter(description = "Admin ID") @PathVariable String adminId) {
        return adminActionService.getActionCount(adminId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed actions", description = "Retrieve all failed actions")
    public Mono<ResponseEntity<List<AdminAction>>> getFailedActions() {
        return adminActionService.getFailedActions()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/permission/{adminId}")
    @Operation(summary = "Check permission", description = "Check if an admin has permission for a specific action type")
    public Mono<ResponseEntity<Boolean>> hasPermission(
            @Parameter(description = "Admin ID") @PathVariable String adminId,
            @Parameter(description = "Action type") @RequestParam AdminAction.ActionType actionType) {
        return adminActionService.hasPermission(adminId, actionType)
                .map(ResponseEntity::ok);
    }
} 