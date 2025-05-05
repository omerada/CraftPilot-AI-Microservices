package com.craftpilot.usermemoryservice.controller;

import com.craftpilot.usermemoryservice.dto.ErrorResponse;
import com.craftpilot.usermemoryservice.dto.UserInstructionRequest;
import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.model.UserInstruction;
import com.craftpilot.usermemoryservice.service.UserInstructionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user-instructions")
@RequiredArgsConstructor
@Slf4j
public class UserInstructionController {
    private final UserInstructionService userInstructionService;

    @PostMapping("/{userId}")
    public Mono<ResponseEntity<Object>> createInstruction(
            @PathVariable String userId,
            @Valid @RequestBody UserInstructionRequest request) {
        
        log.info("Creating instruction for user: {}", userId);
        
        return userInstructionService.createInstruction(userId, request)
                .map(instruction -> ResponseEntity.status(HttpStatus.CREATED).body((Object) instruction))
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
                    log.error("Error creating instruction for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "instruction_creation_error",
                                    "Talimat oluşturulurken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<Object>> getUserInstructions(@PathVariable String userId) {
        log.info("Getting instructions for user: {}", userId);
        
        return userInstructionService.getUserInstructions(userId)
                .collectList()
                .map(instructions -> ResponseEntity.ok().body((Object) instructions))
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
                    log.error("Error getting instructions for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "instruction_retrieval_error",
                                    "Talimatlar alınırken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @PutMapping("/{userId}/{instructionId}")
    public Mono<ResponseEntity<Object>> updateInstruction(
            @PathVariable String userId,
            @PathVariable String instructionId,
            @Valid @RequestBody UserInstructionRequest request) {
        
        log.info("Updating instruction {} for user: {}", instructionId, userId);
        
        return userInstructionService.getInstructionById(instructionId)
                .flatMap(instruction -> {
                    if (!instruction.getUserId().equals(userId)) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body((Object) new ErrorResponse(
                                        "permission_denied",
                                        "Bu talimatı güncelleme izniniz yok.",
                                        HttpStatus.FORBIDDEN.value()
                                )));
                    }
                    
                    return userInstructionService.updateInstruction(instructionId, request)
                            .map(updated -> ResponseEntity.ok().body((Object) updated));
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body((Object) new ErrorResponse(
                                "instruction_not_found",
                                "Belirtilen talimat bulunamadı.",
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
                    log.error("Error updating instruction {} for user {}: {}", instructionId, userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "instruction_update_error",
                                    "Talimat güncellenirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @DeleteMapping("/{userId}/{instructionId}")
    public Mono<ResponseEntity<Object>> deleteInstruction(
            @PathVariable String userId,
            @PathVariable String instructionId) {
        
        log.info("Deleting instruction {} for user: {}", instructionId, userId);
        
        return userInstructionService.getInstructionById(instructionId)
                .flatMap(instruction -> {
                    if (!instruction.getUserId().equals(userId)) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body((Object) new ErrorResponse(
                                        "permission_denied",
                                        "Bu talimatı silme izniniz yok.",
                                        HttpStatus.FORBIDDEN.value()
                                )));
                    }
                    
                    return userInstructionService.deleteInstruction(instructionId)
                            .then(Mono.just(ResponseEntity
                                    .status(HttpStatus.NO_CONTENT)
                                    .build()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body((Object) new ErrorResponse(
                                "instruction_not_found",
                                "Belirtilen talimat bulunamadı.",
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
                    log.error("Error deleting instruction {} for user {}: {}", instructionId, userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "instruction_delete_error",
                                    "Talimat silinirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Object>> deleteAllInstructions(@PathVariable String userId) {
        log.info("Deleting all instructions for user: {}", userId);
        
        return userInstructionService.deleteAllInstructions(userId)
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
                    log.error("Error deleting all instructions for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "instruction_delete_error",
                                    "Talimatlar silinirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }
}
