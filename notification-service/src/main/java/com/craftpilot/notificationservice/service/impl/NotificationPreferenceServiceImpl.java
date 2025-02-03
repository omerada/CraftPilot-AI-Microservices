package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.dto.NotificationPreferenceRequest;
import com.craftpilot.notificationservice.dto.NotificationPreferenceResponse;
import com.craftpilot.notificationservice.model.NotificationPreference;
import com.craftpilot.notificationservice.repository.NotificationPreferenceRepository;
import com.craftpilot.notificationservice.service.NotificationPreferenceService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final MeterRegistry meterRegistry;

    @Override
    @CircuitBreaker(name = "createPreference")
    @Retry(name = "createPreference")
    public Mono<NotificationPreferenceResponse> createPreference(NotificationPreferenceRequest request) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(request.getUserId());
        preference.setChannelPreferences(request.getChannelPreferences());
        preference.setEmail(request.getEmail());
        preference.setDeviceToken(request.getDeviceToken());
        preference.setEmailVerified(false);
        preference.setPhoneVerified(false);
        preference.setAdditionalSettings(request.getAdditionalSettings());
        preference.setActive(true);
        preference.setDeleted(false);
        preference.setCreatedAt(LocalDateTime.now());

        return preferenceRepository.save(preference)
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("preference.created").increment();
                    log.info("Preference created successfully for user: {}", request.getUserId());
                })
                .doOnError(error -> {
                    meterRegistry.counter("preference.creation.failed").increment();
                    log.error("Failed to create preference", error);
                });
    }

    @Override
    public Mono<NotificationPreferenceResponse> getPreference(String id) {
        return preferenceRepository.findById(id)
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> log.debug("Retrieved preference: {}", id));
    }

    @Override
    public Mono<NotificationPreferenceResponse> getUserPreference(String userId) {
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> log.debug("Retrieved preference for user: {}", userId));
    }

    @Override
    public Mono<NotificationPreferenceResponse> updatePreference(String id, NotificationPreferenceRequest request) {
        return preferenceRepository.findById(id)
                .flatMap(preference -> {
                    preference.setChannelPreferences(request.getChannelPreferences());
                    preference.setEmail(request.getEmail());
                    preference.setDeviceToken(request.getDeviceToken());
                    preference.setAdditionalSettings(request.getAdditionalSettings());
                    preference.setUpdatedAt(LocalDateTime.now());
                    return preferenceRepository.save(preference);
                })
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("preference.updated").increment();
                    log.info("Preference updated successfully: {}", id);
                });
    }

    @Override
    public Mono<Void> deletePreference(String id) {
        return preferenceRepository.findById(id)
                .flatMap(preference -> {
                    preference.setDeleted(true);
                    preference.setActive(false);
                    preference.setUpdatedAt(LocalDateTime.now());
                    return preferenceRepository.save(preference);
                })
                .then()
                .doOnSuccess(unused -> {
                    meterRegistry.counter("preference.deleted").increment();
                    log.info("Preference deleted: {}", id);
                });
    }

    @Override
    public Mono<NotificationPreferenceResponse> verifyUserEmail(String userId, String token) {
        // Burada gerçek email doğrulama mantığı uygulanacak
        // Şimdilik sadece email'i doğrulanmış olarak işaretliyoruz
        return preferenceRepository.findByUserId(userId)
                .flatMap(preference -> {
                    preference.setEmailVerified(true);
                    preference.setUpdatedAt(LocalDateTime.now());
                    return preferenceRepository.save(preference);
                })
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("email.verified").increment();
                    log.info("Email verified for user: {}", userId);
                });
    }

    @Override
    public Mono<NotificationPreferenceResponse> verifyUserPhone(String userId, String code) {
        // Burada gerçek telefon doğrulama mantığı uygulanacak
        // Şimdilik sadece telefonu doğrulanmış olarak işaretliyoruz
        return preferenceRepository.findByUserId(userId)
                .flatMap(preference -> {
                    preference.setPhoneVerified(true);
                    preference.setUpdatedAt(LocalDateTime.now());
                    return preferenceRepository.save(preference);
                })
                .map(NotificationPreferenceResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("phone.verified").increment();
                    log.info("Phone verified for user: {}", userId);
                });
    }
} 