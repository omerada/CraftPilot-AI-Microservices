package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.service.ChatHistoryService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    @GetMapping("/histories")
    public Mono<ResponseEntity<Map<String, List<ChatHistory>>>> getChatHistories(@RequestParam String userId) {
        log.info("Sohbet geçmişi istendi, kullanıcı: {}", userId);
        
        return chatHistoryService.getChatHistoriesByUserId(userId)
                .collectList()
                .map(histories -> {
                    log.debug("Bulunan sohbet geçmişi sayısı: {}", histories.size());
                    Map<String, List<ChatHistory>> response = new HashMap<>();
                    response.put("histories", histories);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi alınırken hata: {}", error.getMessage());
                    Map<String, List<ChatHistory>> response = new HashMap<>();
                    response.put("histories", List.of());
                    return Mono.just(ResponseEntity.ok(response));
                });
    }

    @GetMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> getChatHistoryById(@PathVariable String id) {
        log.info("Sohbet geçmişi detayı istendi, ID: {}", id);
        
        return chatHistoryService.getChatHistoryById(id)
                .map(history -> ResponseEntity.ok(history))
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi detayı alınırken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/histories")
    public Mono<ResponseEntity<ChatHistory>> createChatHistory(@RequestBody ChatHistory chatHistory) {
        log.info("Yeni sohbet geçmişi oluşturma isteği: {}", chatHistory.getId());
        
        // String veya Long olarak gelen timestamp değerlerini işle
        processTimestamps(chatHistory);
        
        return chatHistoryService.createChatHistory(chatHistory)
                .map(createdHistory -> {
                    log.debug("Sohbet geçmişi oluşturuldu: {}", createdHistory.getId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(createdHistory);
                })
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi oluşturulurken hata: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistory(@PathVariable String id, @RequestBody ChatHistory chatHistory) {
        log.info("Sohbet geçmişi güncelleme isteği, ID: {}", id);
        
        chatHistory.setId(id);
        return chatHistoryService.updateChatHistory(chatHistory)
                .map(updated -> ResponseEntity.ok(updated))
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi güncellenirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/histories/{id}")
    public Mono<ResponseEntity<Void>> deleteChatHistory(@PathVariable String id) {
        log.info("Sohbet geçmişi silme isteği, ID: {}", id);
        
        return chatHistoryService.deleteChatHistory(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi silinirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/histories/{id}/conversations")
    public Mono<ResponseEntity<ChatHistory>> addConversation(@PathVariable String id, @RequestBody Conversation conversation) {
        log.info("Adding conversation, Chat ID: {}, Role: {}", id, conversation.getRole());
        
        // Validate the conversation
        if (conversation.getContent() == null || conversation.getContent().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(null));
        }
        
        // Ensure ID exists
        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID().toString());
        }
        
        // Simplify by delegating all sequence and timestamp handling to the repository layer
        // This ensures a single source of truth for sequencing
        return chatHistoryService.addConversation(id, conversation)
                .map(updated -> {
                    log.info("Successfully added conversation to chat {}, total conversations: {}", 
                            id, updated.getConversations() != null ? updated.getConversations().size() : 0);
                    return ResponseEntity.ok(updated);
                })
                .onErrorResume(error -> {
                    log.error("Error adding conversation to chat {}: {}", id, error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistoryTitle(@PathVariable String id, @RequestBody TitleUpdateRequest request) {
        log.info("Sohbet başlığı güncelleme isteği, ID: {}, Yeni başlık: {}", id, request.getTitle());
        
        return chatHistoryService.updateChatHistoryTitle(id, request.getTitle())
                .map(updated -> ResponseEntity.ok(updated))
                .onErrorResume(error -> {
                    log.error("Sohbet başlığı güncellenirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    public static class TitleUpdateRequest {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Frontend'den gelen timestamp değerlerini Google Cloud Timestamp'e dönüştürür
     */
    private void processTimestamps(ChatHistory chatHistory) {
        try {
            // createdAt ve updatedAt alanları null ise şu anki zamanı kullan
            if (chatHistory.getCreatedAt() == null) {
                log.debug("createdAt null, şu anki zaman kullanılıyor");
                chatHistory.setCreatedAt(Timestamp.now());
            }
            
            if (chatHistory.getUpdatedAt() == null) {
                log.debug("updatedAt null, şu anki zaman kullanılıyor");
                chatHistory.setUpdatedAt(Timestamp.now());
            }
            
            // Eğer conversations listesi varsa içindeki timestamp değerlerini de işle
            if (chatHistory.getConversations() != null) {
                chatHistory.getConversations().forEach(this::processConversationTimestamp);
            }
        } catch (Exception e) {
            log.error("Timestamp işlenirken hata: {}", e.getMessage());
            // Hata durumunda yeni timestamp'ler oluştur
            chatHistory.setCreatedAt(Timestamp.now());
            chatHistory.setUpdatedAt(Timestamp.now());
        }
    }

    /**
     * Conversation için geliştirilmiş timestamp işleme
     */
    private void processConversationTimestamp(Conversation conversation) {
        try {
            // Sequence değerini öncelikle kontrol et
            Long sequence = conversation.getSequence();
            
            if (sequence != null && sequence > 0) {
                // Sequence varsa, timestamp değerini ondan türet
                conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                    sequence / 1000,
                    (int) ((sequence % 1000) * 1_000_000)
                 ));
                log.debug("Sequence'ten timestamp oluşturuldu: {}", conversation.getTimestamp());
            } 
            else if (conversation.getTimestamp() == null) {
                // Hem sequence hem de timestamp yoksa, şu anki zamanı kullan
                Timestamp now = Timestamp.now();
                conversation.setTimestamp(now);
                
                // Ve bu timestamp'ten bir sequence değeri oluştur
                long seconds = now.getSeconds();
                long nanos = now.getNanos() / 1_000_000; // millisecond kısmını al
                long sequenceValue = seconds * 1000 + nanos;
                conversation.setSequence(sequenceValue);
                
                log.debug("Yeni timestamp ve sequence oluşturuldu: timestamp={}, sequence={}", 
                         now, sequenceValue);
            }
            else {
                // Timestamp var ama sequence yok ise timestamp'ten sequence oluştur
                long seconds = conversation.getTimestamp().getSeconds();
                long nanos = conversation.getTimestamp().getNanos() / 1_000_000;
                long sequenceValue = seconds * 1000 + nanos;
                conversation.setSequence(sequenceValue);
                
                log.debug("Mevcut timestamp'ten sequence oluşturuldu: sequence={}", sequenceValue);
            }
        } catch (Exception e) {
            // Herhangi bir hata olursa güvenli değerler kullan
            log.error("Conversation timestamp işlenirken hata: {}", e.getMessage());
            Timestamp now = Timestamp.now();
            conversation.setTimestamp(now);
            
            long sequenceValue = now.getSeconds() * 1000 + (now.getNanos() / 1_000_000);
            conversation.setSequence(sequenceValue);
            
            log.debug("Hata sonrası yeni değerler atandı: timestamp={}, sequence={}", 
                     now, sequenceValue);
        }
    }
}
