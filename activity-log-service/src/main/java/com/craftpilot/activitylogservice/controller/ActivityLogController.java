package com.craftpilot.activitylogservice.controller;

import com.craftpilot.activitylogservice.model.ActivityLog;
import com.craftpilot.activitylogservice.model.PageResponse;
import com.craftpilot.activitylogservice.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "API to query user activity logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "Get activity logs", description = "Retrieve paginated activity logs with optional filtering")
    public Mono<ResponseEntity<PageResponse<ActivityLog>>> getLogs(
            @RequestHeader(value = "X-User-Id") String currentUserId,
            @RequestHeader(value = "X-User-Role") String currentUserRole,
            
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) String userId,
            
            @Parameter(description = "Filter by action type")
            @RequestParam(required = false) String actionType,
            
            @Parameter(description = "Filter logs after this date (ISO-8601 format)")
            @RequestParam(required = false) String fromDate,
            
            @Parameter(description = "Filter logs before this date (ISO-8601 format)")
            @RequestParam(required = false) String toDate,
            
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            
            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        // For non-admin users, restrict to only their own logs
        String effectiveUserId = userId;
        if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
            effectiveUserId = currentUserId;
        }

        return activityLogService.getLogs(effectiveUserId, actionType, fromDate, toDate, page, size)
                .map(ResponseEntity::ok);
    }
}
