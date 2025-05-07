package com.craftpilot.usermemoryservice.controller;

import com.craftpilot.usermemoryservice.dto.ContextResponse;
import com.craftpilot.usermemoryservice.dto.ErrorResponse;
import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/user-context")
@RequiredArgsConstructor
@Slf4j
public class UserContextController {
    private final UserContextService userContextService;

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<Object>> getUserContext(
            @PathVariable String userId,
            @RequestParam(required = false) String include) {
        
        log.info("Getting user context for user: {} with include: {}", userId, include);
        
        List<String> includeTags = include != null && !include.isEmpty() 
                ? Arrays.asList(include.split(",")) 
                : Collections.emptyList();
        
        return userContextService.getConsolidatedContext(userId, includeTags)
                .map(context -> ResponseEntity.ok().body((Object) context))
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
                    log.error("Error getting context for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "context_retrieval_error",
                                    "Kullanıcı bağlamı alınırken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }
}
