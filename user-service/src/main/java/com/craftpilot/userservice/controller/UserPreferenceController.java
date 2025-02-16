package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.service.UserPreferenceService;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users/{userId}/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    @GetMapping
    public Mono<UserPreference> getUserPreferences(@PathVariable String userId) {
        return userPreferenceService.getUserPreferences(userId);
    }

    @PostMapping
    public Mono<UserPreference> createUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        UserPreference preference = userPreferenceMapper.toEntity(request);
        preference.setUserId(userId);
        return userPreferenceService.saveUserPreferences(preference);
    }

    @PutMapping
    public Mono<UserPreference> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        UserPreference preference = userPreferenceMapper.toEntity(request);
        preference.setUserId(userId);
        return userPreferenceService.saveUserPreferences(preference);
    }

    @DeleteMapping
    public Mono<Void> deleteUserPreferences(@PathVariable String userId) {
        return userPreferenceService.deleteUserPreferences(userId)
                .then();
    }
}