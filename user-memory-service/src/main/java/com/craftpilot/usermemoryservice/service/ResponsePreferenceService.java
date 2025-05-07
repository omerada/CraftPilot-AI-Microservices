package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.dto.ResponsePreferenceRequest;
import com.craftpilot.usermemoryservice.model.ResponsePreference;
import com.craftpilot.usermemoryservice.repository.ResponsePreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponsePreferenceService {
    private final ResponsePreferenceRepository responsePreferenceRepository;

    public Mono<ResponsePreference> savePreferences(String userId, ResponsePreferenceRequest request) {
        log.info("Saving response preferences for userId: {}", userId);
        
        return responsePreferenceRepository.findByUserId(userId)
            .defaultIfEmpty(new ResponsePreference())
            .flatMap(preference -> {
                preference.setUserId(userId);
                preference.setLanguage(request.getLanguage());
                preference.setCommunicationStyle(request.getCommunicationStyle());
                preference.setAdditionalPreferences(request.getAdditionalPreferences());
                preference.setLastUpdated(LocalDateTime.now());
                
                if (preference.getCreated() == null) {
                    preference.setCreated(LocalDateTime.now());
                }
                
                return responsePreferenceRepository.save(preference);
            });
    }

    public Mono<ResponsePreference> getPreferences(String userId) {
        log.info("Getting response preferences for userId: {}", userId);
        return responsePreferenceRepository.findByUserId(userId);
    }

    public Mono<Void> deletePreferences(String userId) {
        log.info("Deleting response preferences for userId: {}", userId);
        return responsePreferenceRepository.deleteByUserId(userId);
    }
}
