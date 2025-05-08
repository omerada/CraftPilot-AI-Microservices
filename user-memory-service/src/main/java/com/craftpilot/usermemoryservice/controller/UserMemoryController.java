package com.craftpilot.usermemoryservice.controller;

import com.craftpilot.usermemoryservice.dto.ErrorResponse;
import com.craftpilot.usermemoryservice.dto.MemoryEntryRequest;
import com.craftpilot.usermemoryservice.dto.MemoryResponse;
import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.service.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/memories")
@RequiredArgsConstructor
@Slf4j
public class UserMemoryController {
    private final UserMemoryService userMemoryService;

    @PostMapping("/entries")
    public Mono<ResponseEntity<Object>> addMemoryEntry(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody MemoryEntryRequest request) {
        
        log.info("Memory entry addition request received for user: {}, content length: {}", 
                userId, request.getContent() != null ? request.getContent().length() : 0);
        
        // İç servis çağrıları için özel kontrol ekleme
        boolean isInternalRequest = true; // LLM servisinden gelen tüm istekleri internal olarak kabul ediyoruz
        
        // Boş içerik kontrolü ekleyelim
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            log.warn("Empty content in memory entry request for user: {}", userId);
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body((Object) new ErrorResponse(
                            "empty_content",
                            "Bellek içeriği boş olamaz",
                            HttpStatus.BAD_REQUEST.value()
                    )));
        }
        
        // Request detaylarını debug log'a ekle
        if (log.isDebugEnabled()) {
            log.debug("Memory entry request details: content={}, source={}, context={}, importance={}",
                    request.getContent(),
                    request.getSource(),
                    request.getContext(),
                    request.getImportance());
        }
        
        // İç servis çağrıları için Firebase yetkilendirme hatasını atlama
        if (isInternalRequest) {
            log.info("Processing internal service request for userId: {}", userId);
            return userMemoryService.addMemoryEntry(userId, request)
                    .map(memory -> ResponseEntity.ok().body((Object) 
                        new MemoryResponse("Memory entry added successfully", true)))
                    .onErrorResume(e -> {
                        log.error("Error adding memory entry for user {}: {}", userId, e.getMessage(), e);
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body((Object) new ErrorResponse(
                                        "memory_processing_error",
                                        "Bellek girişi eklenirken bir hata oluştu: " + e.getMessage(),
                                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                                )));
                    });
        }
        
        // İç servis değilse normal akışa devam et (mevcut kod)
        return userMemoryService.addMemoryEntry(userId, request)
                .map(memory -> {
                    log.info("Memory entry successfully added for user: {}", userId);
                    return ResponseEntity.ok().body((Object) 
                        new MemoryResponse("Memory entry added successfully", true));
                })
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
                    log.error("Error adding memory entry for user {}: {}", userId, e.getMessage(), e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "memory_processing_error",
                                    "Bellek girişi eklenirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<Object>> getUserMemory(@PathVariable String userId) {
        log.info("Request to get memory for user: {}", userId);
        
        return userMemoryService.getUserMemory(userId)
                .map(memory -> ResponseEntity.ok().body((Object) memory))
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
                    log.error("Error retrieving memory for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "memory_retrieval_error",
                                    "Kullanıcı belleği alınırken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }
    
    @DeleteMapping("/{userId}/{entryIndex}")
    public Mono<ResponseEntity<Object>> deleteMemoryEntry(
            @PathVariable String userId,
            @PathVariable int entryIndex) {
        
        log.info("Deleting memory entry at index {} for user: {}", entryIndex, userId);
        
        return userMemoryService.deleteMemoryEntry(userId, entryIndex)
                .map(deleted -> ResponseEntity.ok().body((Object) 
                    new MemoryResponse("Memory entry deleted successfully", true)))
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
                .onErrorResume(IndexOutOfBoundsException.class, e -> {
                    log.error("Index out of bounds for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body((Object) new ErrorResponse(
                                    "index_out_of_bounds",
                                    "Belirtilen indeks geçerli değil: " + e.getMessage(),
                                    HttpStatus.BAD_REQUEST.value()
                            )));
                })
                .onErrorResume(e -> {
                    log.error("Error deleting memory entry for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "memory_deletion_error",
                                    "Bellek girişi silinirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }

    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Object>> deleteAllMemories(@PathVariable String userId) {
        log.info("Deleting all memories for user: {}", userId);
        
        return userMemoryService.deleteAllMemories(userId)
                .then(Mono.just(ResponseEntity.ok().body((Object) 
                    new MemoryResponse("All memory entries deleted successfully", true))))
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
                    log.error("Error deleting all memories for user {}: {}", userId, e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) new ErrorResponse(
                                    "memory_deletion_error",
                                    "Tüm bellek girişleri silinirken bir hata oluştu: " + e.getMessage(),
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                            )));
                });
    }
}
