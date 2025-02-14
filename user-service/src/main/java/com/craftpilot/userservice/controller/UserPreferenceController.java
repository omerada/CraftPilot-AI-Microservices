package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.dto.UserPreferenceResponse;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import com.craftpilot.userservice.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "Kullanıcı Tercihleri", description = "Kullanıcı tercihlerini yönetmek için API")
public class UserPreferenceController {
    
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    @Operation(summary = "Kullanıcı tercihlerini getir")
    @GetMapping("/{userId}")
    public Mono<UserPreferenceResponse> getUserPreferences(@PathVariable String userId) {
        return userPreferenceService.getUserPreferences(userId)
                .map(userPreferenceMapper::toResponse);
    }

    @Operation(summary = "Yeni kullanıcı tercihi kaydet")
    @PostMapping("/{userId}")
    public Mono<UserPreferenceResponse> saveUserPreferences(@PathVariable String userId, 
                                                          @RequestBody @Valid UserPreferenceRequest request) {
        return userPreferenceService.saveUserPreferences(userId, request)
                .map(userPreferenceMapper::toResponse);
    }

    @PutMapping("/{userId}")
    public Mono<UserPreferenceResponse> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody @Valid UserPreferenceRequest request) {
        return userPreferenceService.saveUserPreferences(userId, request)
                .map(userPreferenceMapper::toResponse);
    }

    @DeleteMapping("/{userId}")
    public Mono<Void> deleteUserPreferences(@PathVariable String userId) {
        return userPreferenceService.deleteUserPreferences(userId);
    }
}