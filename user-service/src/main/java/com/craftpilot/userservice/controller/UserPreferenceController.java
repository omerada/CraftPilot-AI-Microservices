package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.UserPreferenceResponse;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {
    
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    @GetMapping("/{userId}")
    public Mono<UserPreferenceResponse> getUserPreferences(@PathVariable String userId) {
        return userPreferenceService.getUserPreferences(userId)
                .map(userPreferenceMapper::toResponse);
    }

    @PutMapping("/{userId}")
    public Mono<UserPreferenceResponse> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreference preferences) {
        return userPreferenceService.updateUserPreferences(userId, preferences)
                .map(userPreferenceMapper::toResponse);
    }

    @DeleteMapping("/{userId}")
    public Mono<Void> deleteUserPreferences(@PathVariable String userId) {
        return userPreferenceService.deleteUserPreferences(userId);
    }
}