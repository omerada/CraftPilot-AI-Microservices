package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.dto.NotificationPreferenceRequest;
import com.craftpilot.notificationservice.dto.NotificationPreferenceResponse;
import reactor.core.publisher.Mono;

public interface NotificationPreferenceService {
    Mono<NotificationPreferenceResponse> createPreference(NotificationPreferenceRequest request);
    Mono<NotificationPreferenceResponse> getPreference(String id);
    Mono<NotificationPreferenceResponse> getUserPreference(String userId);
    Mono<NotificationPreferenceResponse> updatePreference(String id, NotificationPreferenceRequest request);
    Mono<Void> deletePreference(String id);
    Mono<NotificationPreferenceResponse> verifyUserEmail(String userId, String token);
    Mono<NotificationPreferenceResponse> verifyUserPhone(String userId, String code);
} 