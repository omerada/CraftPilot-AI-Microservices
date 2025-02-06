package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing user-related operations using Firebase Authentication and Firestore.
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "Kullanıcı yönetimi için endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni kullanıcı oluştur", description = "Firebase token kullanarak yeni bir kullanıcı oluşturur")
    public Mono<UserEntity> createUser(@RequestHeader("Firebase-Token") String firebaseToken) {
        return userService.verifyAndCreateUser(firebaseToken);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kullanıcı getir", description = "ID'ye göre kullanıcı bilgilerini getirir")
    public Mono<UserEntity> getUser(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kullanıcı güncelle", description = "Kullanıcı bilgilerini günceller")
    public Mono<UserEntity> updateUser(@PathVariable String id, @RequestBody UserEntity updates) {
        return userService.updateUser(id, updates);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Kullanıcı sil", description = "Belirtilen kullanıcıyı siler")
    public Mono<Void> deleteUser(@PathVariable String id) {
        return userService.deleteUser(id);
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
}
