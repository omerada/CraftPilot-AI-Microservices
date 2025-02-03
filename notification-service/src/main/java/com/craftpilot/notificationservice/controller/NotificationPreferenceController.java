package com.craftpilot.notificationservice.controller;

import com.craftpilot.notificationservice.dto.NotificationPreferenceRequest;
import com.craftpilot.notificationservice.dto.NotificationPreferenceResponse;
import com.craftpilot.notificationservice.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preference Controller", description = "Notification preference management endpoints")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create notification preferences")
    public Mono<NotificationPreferenceResponse> createPreference(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return preferenceService.createPreference(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get preference by ID")
    public Mono<NotificationPreferenceResponse> getPreference(@PathVariable String id) {
        return preferenceService.getPreference(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user preferences")
    public Mono<NotificationPreferenceResponse> getUserPreference(@PathVariable String userId) {
        return preferenceService.getUserPreference(userId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification preferences")
    public Mono<NotificationPreferenceResponse> updatePreference(
            @PathVariable String id,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return preferenceService.updatePreference(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete notification preferences")
    public Mono<Void> deletePreference(@PathVariable String id) {
        return preferenceService.deletePreference(id);
    }

    @PostMapping("/verify/email/{userId}")
    @Operation(summary = "Verify user email")
    public Mono<NotificationPreferenceResponse> verifyEmail(
            @PathVariable String userId,
            @RequestParam String token) {
        return preferenceService.verifyUserEmail(userId, token);
    }

    @PostMapping("/verify/phone/{userId}")
    @Operation(summary = "Verify user phone")
    public Mono<NotificationPreferenceResponse> verifyPhone(
            @PathVariable String userId,
            @RequestParam String code) {
        return preferenceService.verifyUserPhone(userId, code);
    }
} 