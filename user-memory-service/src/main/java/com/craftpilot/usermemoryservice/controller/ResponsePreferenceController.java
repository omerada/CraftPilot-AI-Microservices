package com.craftpilot.usermemoryservice.controller;

import com.craftpilot.usermemoryservice.dto.ErrorResponse;
import com.craftpilot.usermemoryservice.dto.ResponsePreferenceRequest;
import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.model.ResponsePreference;
import com.craftpilot.usermemoryservice.service.ResponsePreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/user-preferences/{userId}/language-style")
@RequiredArgsConstructor
@Slf4j
public class ResponsePreferenceController {
    private final ResponsePreferenceService responsePreferenceService;

    @PutMapping
    public Mono<ResponseEntity<Object>> savePreferences(
            @PathVariable String userId,
            @Valid @RequestBody ResponsePreferenceRequest request) {
        
        log.info("Saving response preferences for user: {}", userId);
        
        return responsePreferenceService.savePreferences(userId, request)
                .map(preference -> ResponseEntity.ok().body((Object) preference))
                .onErrorResume(FirebaseAuthException.class, e -> {
                    log.error("Firebase authorization error for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body((Object) new ErrorResponse(
                                    "firebase_auth_error",
                                    "Firebase yetkilendirme hatası. Servis hesabı izinlerini kontrol edin.",
                                    HttpStatus.FORBIDDEN.value()
                            )));
                })
                .onErrorResume(e -> {
                    log.error("Error saving preferences for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "preference_save_error",
                                    "Tercihler kaydedilirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @GetMapping
    public Mono<ResponseEntity<Object>> getPreferences(@PathVariable String userId) {
        log.info("Getting response preferences for user: {}", userId);
        
        return responsePreferenceService.getPreferences(userId)
                .map(preference -> ResponseEntity.ok().body((Object) preference))
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body((Object) new ErrorResponse(
                                "preferences_not_found",
                                "Kullanıcı için dil ve stil tercihleri bulunamadı.",
                                HttpStatus.NOT_FOUND.value()
                        ))))
                .onErrorResume(FirebaseAuthException.class, e -> {
                    log.error("Firebase authorization error for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body((Object) new ErrorResponse(
                                    "firebase_auth_error",
                                    "Firebase yetkilendirme hatası. Servis hesabı izinlerini kontrol edin.",
                                    HttpStatus.FORBIDDEN.value()
                            )));
                })
                .onErrorResume(e -> {
                    log.error("Error getting preferences for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "preference_retrieval_error",
                                    "Tercihler alınırken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @DeleteMapping
    public Mono<ResponseEntity<Object>> deletePreferences(@PathVariable String userId) {
        log.info("Deleting response preferences for user: {}", userId);
        
        return responsePreferenceService.deletePreferences(userId)
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()))
                .onErrorResume(FirebaseAuthException.class, e -> {
                    log.error("Firebase authorization error for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body((Object) new ErrorResponse(
                                    "firebase_auth_error",
                                    "Firebase yetkilendirme hatası. Servis hesabı izinlerini kontrol edin.",
                                    HttpStatus.FORBIDDEN.value()
                            )));
                })
                .onErrorResume(e -> {
                    log.error("Error deleting preferences for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "preference_delete_error",
                                    "Tercihler silinirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }
}
