package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.service.UserPreferenceService;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/users/{userId}/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Preferences", description = "Kullanıcı tercihlerini yönetme API'leri")
public class UserPreferenceController {
    
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    @GetMapping
    @Operation(summary = "Kullanıcı tercihlerini getir", description = "Kullanıcıya ait tüm tercihleri getirir")
    public Mono<ResponseEntity<UserPreference>> getUserPreferences(@PathVariable String userId) {
        log.info("Kullanıcı tercihleri isteniyor: userId={}", userId);
        return userPreferenceService.getUserPreferences(userId)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Kullanıcı tercihleri başarıyla getirildi: userId={}", userId))
                .doOnError(e -> log.error("Kullanıcı tercihleri getirilirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @PostMapping
    @Operation(summary = "Kullanıcı tercihleri oluştur", description = "Yeni kullanıcı tercihleri oluşturur")
    public Mono<ResponseEntity<UserPreference>> createUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        log.info("Kullanıcı tercihleri oluşturuluyor: userId={}", userId);
        
        try {
            UserPreference preference = userPreferenceMapper.toEntity(request);
            preference.setUserId(userId);
            
            Map<String, Boolean> notificationsMap = new HashMap<>();
            notificationsMap.put("general", true); // veya gerekli değer
            preference.setNotifications(notificationsMap);
            
            return userPreferenceService.saveUserPreferences(preference)
                    .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                    .doOnSuccess(response -> log.info("Kullanıcı tercihleri başarıyla oluşturuldu: userId={}", userId))
                    .doOnError(e -> log.error("Kullanıcı tercihleri oluşturulurken hata: userId={}, error={}", userId, e.getMessage()))
                    .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
        } catch (Exception e) {
            log.error("İstek işlenirken beklenmeyen hata: userId={}, error={}", userId, e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
    }

    @PutMapping
    @Operation(summary = "Kullanıcı tercihlerini güncelle", description = "Kullanıcı tercihlerini günceller")
    public Mono<ResponseEntity<UserPreference>> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {
        log.info("Kullanıcı tercihleri güncelleniyor: userId={}", userId);
        
        return userPreferenceService.updateUserPreferences(userId, updates)
                .timeout(Duration.ofMillis(1500))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Kullanıcı tercihleri başarıyla güncellendi: userId={}", userId))
                .doOnError(e -> {
                    if (e instanceof TimeoutException) {
                        log.warn("Kullanıcı tercihleri güncelleme zaman aşımına uğradı: userId={}", userId);
                    } else {
                        log.error("Kullanıcı tercihleri güncellenirken hata: userId={}, error={}", userId, e.getMessage());
                    }
                })
                .onErrorResume(TimeoutException.class, e -> {
                    UserPreference preference = createFallbackPreference(userId, updates);
                    return Mono.just(ResponseEntity.accepted().body(preference));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @DeleteMapping
    @Operation(summary = "Kullanıcı tercihlerini sil", description = "Kullanıcı tercihlerini siler")
    public Mono<ResponseEntity<Void>> deleteUserPreferences(@PathVariable String userId) {
        log.info("Kullanıcı tercihleri siliniyor: userId={}", userId);
        return userPreferenceService.deleteUserPreferences(userId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnSuccess(response -> log.info("Kullanıcı tercihleri başarıyla silindi: userId={}", userId))
                .doOnError(e -> log.error("Kullanıcı tercihleri silinirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @PutMapping("/theme")
    @Operation(summary = "Tema tercihini güncelle", description = "Kullanıcının tema tercihini günceller")
    public Mono<ResponseEntity<UserPreference>> updateTheme(
            @PathVariable String userId,
            @RequestParam String theme) {
        log.info("Tema güncelleme isteği: userId={}, theme={}", userId, theme);
        return userPreferenceService.updateTheme(userId, theme)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Tema başarıyla güncellendi: userId={}, theme={}", userId, theme))
                .doOnError(e -> log.error("Tema güncellenirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Tema güncelleme hatası (fallback çalıştırılıyor): {}", e.getMessage());
                    return createFallbackThemePreference(userId, theme)
                            .map(fallback -> ResponseEntity.ok(fallback))
                            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/language")
    @Operation(summary = "Dil tercihini güncelle", description = "Kullanıcının dil tercihini günceller")
    public Mono<ResponseEntity<UserPreference>> updateLanguage(
            @PathVariable String userId,
            @RequestBody Map<String, String> requestBody) {
        String language = requestBody.get("language");
        if (language == null || language.isEmpty()) {
            log.warn("Geçersiz dil parametresi: userId={}", userId);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        log.info("Dil güncelleme isteği: userId={}, language={}", userId, language);
        return userPreferenceService.updateLanguage(userId, language)
                .map(ResponseEntity::ok)
                .timeout(java.time.Duration.ofSeconds(8))
                .doOnSuccess(response -> log.info("Dil başarıyla güncellendi: userId={}, language={}", userId, language))
                .doOnError(e -> log.error("Dil güncellenirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Dil güncelleme hatası (fallback çalıştırılıyor): {}", e.getMessage());
                    return createFallbackLanguagePreference(userId, language)
                            .map(fallback -> ResponseEntity.ok(fallback))
                            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/ai-model-favorites")
    @Operation(summary = "Favori modelleri güncelle", description = "Kullanıcının favori AI modellerini günceller")
    public Mono<ResponseEntity<UserPreference>> updateAiModelFavorites(
            @PathVariable String userId,
            @RequestBody List<String> favorites) {
        log.info("Favori modeller güncelleniyor: userId={}, favorites={}", userId, favorites);
        return userPreferenceService.updateAiModelFavorites(userId, favorites)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Favori modeller başarıyla güncellendi: userId={}", userId))
                .doOnError(e -> log.error("Favori modeller güncellenirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Favori modeller güncelleme hatası (fallback çalıştırılıyor): {}", e.getMessage());
                    return createFallbackFavoritesPreference(userId, favorites)
                            .map(fallback -> ResponseEntity.ok(fallback))
                            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/ai-model-favorites")
    @Operation(summary = "Favori modelleri getir", description = "Kullanıcının favori AI modellerini getirir")
    public Mono<ResponseEntity<List<String>>> getAiModelFavorites(@PathVariable String userId) {
        log.info("Favori modeller isteniyor: userId={}", userId);
        return userPreferenceService.getUserPreferences(userId)
                .map(UserPreference::getAiModelFavorites)
                .defaultIfEmpty(List.of())
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Favori modeller başarıyla getirildi: userId={}", userId))
                .doOnError(e -> log.error("Favori modeller getirilirken hata: userId={}, error={}", userId, e.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.ok(List.of())));
    }

    // Fallback metotları
    private Mono<UserPreference> createFallbackPreference(String userId, UserPreferenceRequest request) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true);
        
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme(request.getTheme() != null ? request.getTheme() : "light")
                .language(request.getLanguage() != null ? request.getLanguage() : "tr")
                .notifications(request.getNotifications() != null ? request.getNotifications() : notificationsMap)
                .pushEnabled(request.getPushEnabled() != null ? request.getPushEnabled() : true)
                .aiModelFavorites(request.getAiModelFavorites() != null ? request.getAiModelFavorites() : List.of())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback tercihi oluşturuldu: userId={}", userId);
        return Mono.just(fallbackPreference);
    }

    private Mono<UserPreference> createFallbackThemePreference(String userId, String theme) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true);
        
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme(theme)
                .language("tr")
                .notifications(notificationsMap)
                .pushEnabled(true)
                .aiModelFavorites(List.of())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback tema tercihi oluşturuldu: userId={}, theme={}", userId, theme);
        return Mono.just(fallbackPreference);
    }

    private Mono<UserPreference> createFallbackLanguagePreference(String userId, String language) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true);
        
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language(language)
                .notifications(notificationsMap)
                .pushEnabled(true)
                .aiModelFavorites(List.of())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback dil tercihi oluşturuldu: userId={}, language={}", userId, language);
        return Mono.just(fallbackPreference);
    }

    private Mono<UserPreference> createFallbackFavoritesPreference(String userId, List<String> favorites) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true);
        
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(notificationsMap)
                .pushEnabled(true)
                .aiModelFavorites(favorites)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback favori modeller tercihi oluşturuldu: userId={}", userId);
        return Mono.just(fallbackPreference);
    }

    // Fallback metodu - zaman aşımı durumunda
    private UserPreference createFallbackPreference(String userId, Map<String, Object> updates) {
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme(updates.containsKey("theme") ? (String)updates.get("theme") : "system")
                .themeSchema(updates.containsKey("themeSchema") ? (String)updates.get("themeSchema") : "default")
                .language(updates.containsKey("language") ? (String)updates.get("language") : "tr")
                .layout(updates.containsKey("layout") ? (String)updates.get("layout") : "collapsibleSide")
                .pushEnabled(updates.containsKey("pushEnabled") ? (Boolean)updates.get("pushEnabled") : false)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        if (updates.containsKey("aiModelFavorites")) {
            fallbackPreference.setAiModelFavorites((List<String>)updates.get("aiModelFavorites"));
        }
        
        if (updates.containsKey("notifications")) {
            fallbackPreference.setNotifications((Map<String, Boolean>)updates.get("notifications"));
        }
        
        log.info("Fallback tercihi oluşturuldu: userId={}", userId);
        return fallbackPreference;
    }
}