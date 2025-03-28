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
        log.info("Sohbete mesaj ekleme isteği, Chat ID: {}, Sequence: {}, Role: {}", id, conversation.getSequence(), conversation.getRole());
        
        // Timestamp değerini düzelt
        processConversationTimestamp(conversation);
        
        // Sequence yoksa veya 0 ise rol ve zaman damgasına göre uygun bir değer atama
        // Daha agresif kontrol yapıyoruz
        if (conversation.getSequence() == null || conversation.getSequence() == 0) {
            // Şu anki zamanı milisaniye olarak al
            long currentTime = System.currentTimeMillis();
            
            // Eğer bu kullanıcı mesajı ise, çok daha geriye dönük bir timestamp ver
            // Bu sayede kullanıcı mesajı her zaman AI yanıtından önce gelecek
            if ("user".equals(conversation.getRole())) {
                // İlk mesaj olabilir - çok daha eski bir timestamp ver (1 saat önce)
                conversation.setSequence(currentTime - 3600000); // 1 saat öncesi
                log.info("USER mesajı için sequence atandı: {}", conversation.getSequence());
            } else {
                conversation.setSequence(currentTime);
                log.info("AI mesajı için sequence atandı: {}", conversation.getSequence());
            }
        }
        
        // Timestamp değerini sequence ile uyumlu hale getir
        if (conversation.getTimestamp() == null) {
            conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                conversation.getSequence() / 1000,
                (int) ((conversation.getSequence() % 1000) * 1_000_000)
            ));
        }
        
        log.info("Sohbete mesaj ekleniyor, Chat ID: {}, Sequence: {}", id, conversation.getSequence());
        return chatHistoryService.addConversation(id, conversation)
                .map(updated -> {
                    log.debug("Mesaj eklendi, Chat ID: {}", id);
                    return ResponseEntity.ok(updated);
                })
                .onErrorResume(error -> {
                    log.error("Mesaj eklenirken hata, Chat ID {}: {}", id, error.getMessage());
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
     * Conversation için timestamp işleme
     */
    private void processConversationTimestamp(Conversation conversation) {
        try {
            if (conversation.getTimestamp() == null) {
                log.debug("Conversation timestamp null, şu anki zaman kullanılıyor");
                conversation.setTimestamp(Timestamp.now());
            }
        } catch (Exception e) {
            log.error("Conversation timestamp işlenirken hata: {}", e.getMessage());
            conversation.setTimestamp(Timestamp.now());
        }
    }
}
