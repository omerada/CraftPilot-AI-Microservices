package com.craftpilot.notificationservice.controller;

import com.craftpilot.notificationservice.dto.NotificationRequest;
import com.craftpilot.notificationservice.dto.NotificationResponse;
import com.craftpilot.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Controller", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new notification")
    public Mono<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        return notificationService.createNotification(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public Mono<NotificationResponse> getNotification(@PathVariable String id) {
        return notificationService.getNotification(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications")
    public Flux<NotificationResponse> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean onlyUnread) {
        return notificationService.getUserNotifications(userId, onlyUnread);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public Mono<NotificationResponse> markAsRead(@PathVariable String id) {
        return notificationService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete notification")
    public Mono<Void> deleteNotification(@PathVariable String id) {
        return notificationService.deleteNotification(id);
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Get all scheduled notifications")
    public Flux<NotificationResponse> getScheduledNotifications() {
        return notificationService.getScheduledNotifications()
                .map(NotificationResponse::fromEntity);
    }
} 