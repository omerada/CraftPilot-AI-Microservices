package com.craftpilot.notificationservice.controller;

import com.craftpilot.notificationservice.dto.EmailRequest;
import com.craftpilot.notificationservice.dto.NotificationRequest;
import com.craftpilot.notificationservice.dto.NotificationResponse;
import com.craftpilot.notificationservice.service.NotificationService;
import com.craftpilot.notificationservice.service.impl.FCMNotificationService;
import com.craftpilot.notificationservice.service.impl.GmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Controller", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final FCMNotificationService fcmNotificationService;
    private final GmailService gmailService;

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

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request) {
        if (request.getToken() != null) {
            fcmNotificationService.sendNotification(request.getToken(), request.getTitle(), request.getBody());
        } else if (request.getTokens() != null && !request.getTokens().isEmpty()) {
            fcmNotificationService.sendMulticastNotification(request.getTokens(), request.getTitle(), request.getBody());
        } else if (request.getTopic() != null) {
            fcmNotificationService.sendTopicNotification(request.getTopic(), request.getTitle(), request.getBody());
        } else {
            return ResponseEntity.badRequest().body("Token, tokens veya topic gereklidir");
        }
        return ResponseEntity.ok("Bildirim başarıyla gönderildi");
    }

    @PostMapping("/email")
    @Operation(summary = "Send email notification")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            gmailService.sendEmail(request.getTo(), request.getSubject(), request.getBody());
            return ResponseEntity.ok("E-posta başarıyla gönderildi");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("E-posta gönderilirken hata oluştu: " + e.getMessage());
        }
    }
} 