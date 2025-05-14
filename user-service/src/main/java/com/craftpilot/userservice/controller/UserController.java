package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.User;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for managing user-related operations using JWT/OAuth
 * Authentication and MongoDB.
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "Kullanıcı yönetimi için endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni kullanıcı oluştur", description = "Token kullanarak yeni bir kullanıcı oluşturur ve benzersiz bir kullanıcı adı atar")
    public Mono<UserEntity> createUser(@RequestHeader("Authorization") String authToken) {
        log.info("Yeni kullanıcı oluşturma isteği alındı");
        return userService.verifyAndCreateUser(authToken)
                .doOnSuccess(user -> log.info("Kullanıcı başarıyla oluşturuldu: id={}, username={}", user.getId(),
                        user.getUsername()))
                .doOnError(e -> log.error("Kullanıcı oluşturulurken hata: {}", e.getMessage()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kullanıcı getir", description = "ID'ye göre kullanıcı bilgilerini getirir")
    public Mono<ResponseEntity<Object>> getUser(@PathVariable String id) {
        log.info("Kullanıcı bilgisi isteniyor: id={}", id);

        return userService.findById(id)
                .map(user -> ResponseEntity.ok().body((Object) user))
                .doOnSuccess(responseEntity -> {
                    if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                        log.info("Kullanıcı başarıyla getirildi: id={}", id);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Kullanıcı bulunamadı: id={}", id);
                    Map<String, String> errorResponse = Map.of(
                            "error", "User not found",
                            "message", "Belirtilen ID ile kullanıcı bulunamadı: " + id,
                            "status", "404");
                    return Mono.<ResponseEntity<Object>>just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
                }))
                .onErrorResume(e -> {
                    log.error("Kullanıcı getirilirken hata: id={}, error={}", id, e.getMessage());
                    Map<String, String> errorResponse = Map.of(
                            "error", "Internal Server Error",
                            "message", "Kullanıcı bilgisi alınırken bir hata oluştu",
                            "status", "500");
                    return Mono.<ResponseEntity<Object>>just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kullanıcı güncelle", description = "Kullanıcı bilgilerini günceller ve kullanıcı adı değiştiriliyorsa benzersizliği kontrol eder")
    public Mono<ResponseEntity<Object>> updateUser(@PathVariable String id, @RequestBody UserEntity updates) {
        log.info("Kullanıcı güncelleme isteği alındı: id={}", id);
        return userService.findById(id)
                .flatMap(existingUser -> userService.updateUser(id, updates)
                        .map(updatedUser -> ResponseEntity.ok().body((Object) updatedUser)))
                .doOnSuccess(responseEntity -> {
                    if (responseEntity.getStatusCode().is2xxSuccessful()) {
                        log.info("Kullanıcı başarıyla güncellendi: id={}", id);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Güncellenecek kullanıcı bulunamadı: id={}", id);
                    Map<String, String> errorResponse = Map.of(
                            "error", "User not found",
                            "message", "Belirtilen ID ile güncellenecek kullanıcı bulunamadı: " + id,
                            "status", "404");
                    return Mono.<ResponseEntity<Object>>just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
                }))
                .onErrorResume(e -> {
                    log.error("Kullanıcı güncellenirken hata: id={}, error={}", id, e.getMessage());
                    Map<String, String> errorResponse = Map.of(
                            "error", "Internal Server Error",
                            "message", "Kullanıcı güncellenirken bir hata oluştu: " + e.getMessage(),
                            "status", "500");
                    return Mono.<ResponseEntity<Object>>just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kullanıcı sil", description = "Belirtilen kullanıcıyı, tercihlerini ve Firebase'deki kaydını siler")
    public Mono<ResponseEntity<Object>> deleteUser(@PathVariable String id) {
        log.info("Kullanıcı silme isteği alındı: id={}", id);
        return userService.findById(id)
                .flatMap(user -> userService.deleteUser(id)
                        .then(Mono.just(ResponseEntity.noContent().<Object>build())))
                .doOnSuccess(response -> log.info("Kullanıcı ve ilgili verileri başarıyla silindi: id={}", id))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Silinecek kullanıcı bulunamadı: id={}", id);
                    Map<String, String> errorResponse = Map.of(
                            "error", "User not found",
                            "message", "Belirtilen ID ile silinecek kullanıcı bulunamadı: " + id,
                            "status", "404");
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) errorResponse));
                }))
                .onErrorResume(e -> {
                    log.error("Kullanıcı silinirken hata: id={}, error={}", id, e.getMessage());
                    Map<String, String> errorResponse = Map.of(
                            "error", "Internal Server Error",
                            "message", "Kullanıcı silinirken bir hata oluştu: " + e.getMessage(),
                            "status", "500");
                    return Mono
                            .just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse));
                });
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Kullanıcı durumunu güncelle", description = "Kullanıcının durumunu günceller")
    public Mono<UserEntity> updateUserStatus(
            @PathVariable String id,
            @RequestParam String status) {
        try {
            UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
            return userService.updateUserStatus(id, userStatus);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid status: " + status));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Kullanıcı ara", description = "Email veya kullanıcı adına göre kullanıcı arar")
    public Mono<UserEntity> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username) {
        return userService.searchUsers(email, username);
    }

    @PostMapping("/sync")
    @Operation(summary = "Kullanıcıyı senkronize et", description = "Authentication'dan gelen kullanıcıyı sistemle senkronize eder")
    public Mono<UserEntity> syncUser(@RequestHeader("Authorization") String authToken) {
        log.info("Kullanıcı senkronizasyon isteği alındı");
        return userService.verifyAndCreateOrUpdateUser(authToken)
                .doOnSuccess(user -> log.info("Kullanıcı başarıyla senkronize edildi: id={}", user.getId()))
                .doOnError(e -> log.error("Kullanıcı senkronizasyonunda hata: {}", e.getMessage()));
    }

    @PutMapping("/{id}/firebase-sync")
    @Operation(summary = "Firebase güncellemelerini senkronize et", description = "Firebase'de yapılan güncellemeleri sistemle senkronize eder")
    public Mono<UserEntity> syncFirebaseUpdates(
            @PathVariable String id,
            @RequestBody UserEntity updates) {
        return userService.handleFirebaseUpdate(id, updates);
    }
}
