package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.service.UserPreferenceService;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Kullanıcı tercihlerini yönetme API'leri")
public class UserPreferenceController {
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    @GetMapping
    @Operation(summary = "Kullanıcı tercihlerini getir", description = "Kullanıcıya ait tüm tercihleri getirir")
    public Mono<UserPreference> getUserPreferences(@PathVariable String userId) {
        return userPreferenceService.getUserPreferences(userId);
    }

    @PostMapping
    @Operation(summary = "Kullanıcı tercihleri oluştur", description = "Yeni kullanıcı tercihleri oluşturur")
    public Mono<UserPreference> createUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        UserPreference preference = userPreferenceMapper.toEntity(request);
        preference.setUserId(userId);
        return userPreferenceService.saveUserPreferences(preference);
    }

    @PutMapping
    @Operation(summary = "Kullanıcı tercihlerini güncelle", description = "Kullanıcı tercihlerini günceller")
    public Mono<UserPreference> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        UserPreference preference = userPreferenceMapper.toEntity(request);
        preference.setUserId(userId);
        return userPreferenceService.saveUserPreferences(preference);
    }

    @DeleteMapping
    @Operation(summary = "Kullanıcı tercihlerini sil", description = "Kullanıcı tercihlerini siler")
    public Mono<Void> deleteUserPreferences(@PathVariable String userId) {
        return userPreferenceService.deleteUserPreferences(userId)
                .then();
    }

    // Tema tercihi için özel endpoint
    @PutMapping("/theme")
    @Operation(summary = "Tema tercihini güncelle", description = "Kullanıcının tema tercihini günceller")
    public Mono<UserPreference> updateTheme(
            @PathVariable String userId,
            @RequestParam String theme) {
        return userPreferenceService.updateTheme(userId, theme);
    }

    // Dil tercihi için özel endpoint
    @PutMapping("/language")
    @Operation(summary = "Dil tercihini güncelle", description = "Kullanıcının dil tercihini günceller")
    public Mono<UserPreference> updateLanguage(
            @PathVariable String userId,
            @RequestParam String language) {
        return userPreferenceService.updateLanguage(userId, language);
    }

    // Favori modeller için özel endpoint
    @PutMapping("/ai-model-favorites")
    @Operation(summary = "Favori modelleri güncelle", description = "Kullanıcının favori AI modellerini günceller")
    public Mono<UserPreference> updateAiModelFavorites(
            @PathVariable String userId,
            @RequestBody List<String> favorites) {
        return userPreferenceService.updateAiModelFavorites(userId, favorites);
    }

    // Favori modelleri getirme endpointi
    @GetMapping("/ai-model-favorites")
    @Operation(summary = "Favori modelleri getir", description = "Kullanıcının favori AI modellerini getirir")
    public Mono<List<String>> getAiModelFavorites(@PathVariable String userId) {
        return userPreferenceService.getUserPreferences(userId)
                .map(UserPreference::getAiModelFavorites)
                .defaultIfEmpty(List.of());
    }
}