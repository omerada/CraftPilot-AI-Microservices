package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.repository.ChatHistoryRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;

    public Flux<ChatHistory> getChatHistoriesByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("Geçersiz userId ile sohbet geçmişi istendi: {}", userId);
            return Flux.empty();
        }
        
        log.debug("Kullanıcı için sohbet geçmişleri getiriliyor: {}", userId);
        return chatHistoryRepository.findAllByUserId(userId)
                .doOnError(error -> log.error("Sohbet geçmişi getirirken hata: {}", error.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    public Mono<ChatHistory> getChatHistoryById(String id) {
        if (id == null || id.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi istendi");
            return Mono.empty();
        }
        
        log.debug("Sohbet geçmişi getiriliyor, ID: {}", id);
        return chatHistoryRepository.findById(id)
                .doOnError(error -> log.error("ID ile sohbet geçmişi getirirken hata, ID {}: {}", id, error.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<ChatHistory> createChatHistory(ChatHistory chatHistory) {
        if (chatHistory == null) {
            log.warn("Null sohbet geçmişi oluşturma isteği");
            return Mono.error(new IllegalArgumentException("Sohbet geçmişi boş olamaz"));
        }
        
        // ID yoksa oluştur
        if (chatHistory.getId() == null) {
            chatHistory.setId(UUID.randomUUID().toString());
        }
        
        // Timestamp kontrolü
        if (chatHistory.getCreatedAt() == null) {
            chatHistory.setCreatedAt(Timestamp.now());
        }
        if (chatHistory.getUpdatedAt() == null) {
            chatHistory.setUpdatedAt(Timestamp.now());
        }
        
        // Conversations null ise initialize et
        if (chatHistory.getConversations() == null) {
            chatHistory.setConversations(new ArrayList<>());
        }
        
        log.debug("Sohbet geçmişi oluşturuluyor: {}", chatHistory.getId());
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi oluştururken hata: {}", error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi oluşturulamadı: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> updateChatHistory(ChatHistory chatHistory) {
        if (chatHistory == null || chatHistory.getId() == null) {
            log.warn("Geçersiz sohbet geçmişi güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir sohbet geçmişi ve ID gerekli"));
        }
        
        // UpdatedAt'i her zaman güncelle
        chatHistory.setUpdatedAt(Timestamp.now());
        
        log.debug("Sohbet geçmişi güncelleniyor, ID: {}", chatHistory.getId());
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi güncellenirken hata, ID {}: {}", chatHistory.getId(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi güncellenemedi: " + e.getMessage(), e));
    }

    public Mono<Void> deleteChatHistory(String id) {
        if (id == null || id.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi silme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
        
        log.debug("Sohbet geçmişi siliniyor, ID: {}", id);
        return chatHistoryRepository.delete(id)
                .doOnError(error -> log.error("Sohbet geçmişi silinirken hata, ID {}: {}", id, error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi silinemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> addConversation(String historyId, Conversation conversation) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz sohbet ID ile mesaj ekleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir sohbet ID'si gerekli"));
        }
        
        if (conversation == null) {
            log.warn("Null konuşma ekleme isteği, Chat ID: {}", historyId);
            return Mono.error(new IllegalArgumentException("Geçerli bir mesaj gerekli"));
        }
        
        // Conversation ID yoksa oluştur
        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID().toString());
        }
        
        // Timestamp değeri yoksa, şu anki zamanı ekle
        if (conversation.getTimestamp() == null) {
            conversation.setTimestamp(Timestamp.now());
        }
        
        log.debug("Sohbete mesaj ekleniyor, Chat ID: {}, OrderIndex: {}", historyId, conversation.getOrderIndex());
        return chatHistoryRepository.addConversation(historyId, conversation)
                .doOnSuccess(result -> log.info("Mesaj başarıyla eklendi, Chat ID: {}, OrderIndex: {}", 
                                          historyId, conversation.getOrderIndex()))
                .doOnError(error -> log.error("Mesaj eklenirken hata, Chat ID {}, OrderIndex {}: {}", 
                                        historyId, conversation.getOrderIndex(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Mesaj eklenemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> updateChatHistoryTitle(String historyId, String newTitle) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet başlığı güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
        
        if (newTitle == null) {
            log.warn("Null başlık ile sohbet başlığı güncelleme isteği, ID: {}", historyId);
            newTitle = "Yeni Sohbet"; // Varsayılan başlık
        }
        
        log.debug("Sohbet başlığı güncelleniyor, ID: {}, Yeni başlık: {}", historyId, newTitle);
        return chatHistoryRepository.updateTitle(historyId, newTitle)
                .doOnError(error -> log.error("Sohbet başlığı güncellenirken hata, ID {}: {}", historyId, error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet başlığı güncellenemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> archiveChatHistory(String historyId) {
        log.info("Sohbet arşivleniyor, ID: {}", historyId);
        
        return chatHistoryRepository.findById(historyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Chat history with ID " + historyId + " not found")))
                .flatMap(chatHistory -> {
                    log.debug("Sohbet geçmişi bulundu, şu anki enable değeri: {}", chatHistory.isEnable());
                    chatHistory.setEnable(false);
                    chatHistory.setUpdatedAt(Timestamp.now());
                    
                    return chatHistoryRepository.save(chatHistory)
                            .doOnSuccess(updatedChat -> 
                                log.info("Sohbet başarıyla arşivlendi, ID: {}", updatedChat.getId()));
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        log.error("Sohbet arşivlenemedi: {}", e.getMessage());
                        return Mono.error(e);
                    }
                    log.error("Sohbet arşivlenirken hata: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Sohbet arşivlenemedi: " + e.getMessage(), e));
                });
    }
}
