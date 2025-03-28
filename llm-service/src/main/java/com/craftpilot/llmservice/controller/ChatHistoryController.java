package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.service.ChatHistoryService;
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
        log.info("Sohbete mesaj ekleme isteği, Chat ID: {}", id);
        
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
}
