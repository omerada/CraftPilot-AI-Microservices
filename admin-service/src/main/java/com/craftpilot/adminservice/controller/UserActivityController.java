package com.craftpilot.adminservice.controller;

import com.craftpilot.adminservice.model.UserActivity;
import com.craftpilot.adminservice.service.UserActivityService;
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
@RequestMapping("/user-activities")
@RequiredArgsConstructor
@Tag(name = "User Activities", description = "User activity management APIs")
public class UserActivityController {
    private final UserActivityService userActivityService;

    @PostMapping
    @Operation(summary = "Record activity", description = "Record a new user activity")
    public Mono<ResponseEntity<UserActivity>> recordActivity(
            @RequestBody UserActivity activity) {
        return userActivityService.recordActivity(activity)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Retrieve a specific user activity")
    public Mono<ResponseEntity<UserActivity>> getActivityById(
            @Parameter(description = "Activity ID") @PathVariable String id) {
        return userActivityService.getActivityById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user activities", description = "Retrieve all activities for a specific user")
    public Flux<UserActivity> getUserActivities(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return userActivityService.getUserActivities(userId);
    }

    @GetMapping("/type/{activityType}")
    @Operation(summary = "Get activities by type", description = "Retrieve activities of a specific type")
    public Flux<UserActivity> getActivitiesByType(
            @Parameter(description = "Activity type") @PathVariable UserActivity.ActivityType activityType) {
        return userActivityService.getActivitiesByType(activityType);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get activities by status", description = "Retrieve activities with a specific status")
    public Flux<UserActivity> getActivitiesByStatus(
            @Parameter(description = "Activity status") @PathVariable UserActivity.ActivityStatus status) {
        return userActivityService.getActivitiesByStatus(status);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get activities by time range", description = "Retrieve activities within a specific time range")
    public Flux<UserActivity> getActivitiesByTimeRange(
            @Parameter(description = "Start time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return userActivityService.getActivitiesByTimeRange(start, end);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity", description = "Delete a specific user activity")
    public Mono<ResponseEntity<Void>> deleteActivity(
            @Parameter(description = "Activity ID") @PathVariable String id) {
        return userActivityService.deleteActivity(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/suspicious")
    @Operation(summary = "Get suspicious activities", description = "Retrieve all suspicious activities")
    public Mono<ResponseEntity<List<UserActivity>>> getSuspiciousActivities() {
        return userActivityService.getSuspiciousActivities()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get blocked activities", description = "Retrieve all blocked activities")
    public Mono<ResponseEntity<List<UserActivity>>> getBlockedActivities() {
        return userActivityService.getBlockedActivities()
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update activity status", description = "Update the status of a specific activity")
    public Mono<ResponseEntity<UserActivity>> updateActivityStatus(
            @Parameter(description = "Activity ID") @PathVariable String id,
            @Parameter(description = "New status") @RequestParam UserActivity.ActivityStatus newStatus) {
        return userActivityService.updateActivityStatus(id, newStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent activities", description = "Retrieve activities since a specific time")
    public Mono<ResponseEntity<List<UserActivity>>> getRecentActivities(
            @Parameter(description = "Since time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return userActivityService.getRecentActivities(since)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/count/{userId}")
    @Operation(summary = "Get activity count", description = "Get the total number of activities for a user")
    public Mono<ResponseEntity<Long>> getActivityCount(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return userActivityService.getActivityCount(userId)
                .map(ResponseEntity::ok);
    }
} 