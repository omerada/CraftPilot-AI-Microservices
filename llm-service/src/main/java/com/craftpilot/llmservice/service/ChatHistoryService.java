package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.repository.ChatHistoryRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        
        // Sequence değeri yoksa veya 0 ise, daha güvenli bir değer atama
        if (conversation.getSequence() == null || conversation.getSequence() == 0) {
            long currentTime = System.currentTimeMillis();
            
            if ("user".equals(conversation.getRole())) {
                // User mesajları için her zaman daha düşük sequence (10 saniye öncesi)
                conversation.setSequence(currentTime - 10000);
            } else {
                // Assistant mesajları için güncel zaman
                conversation.setSequence(currentTime);
            }
            
            // Timestamp'i sequence ile senkronize et
            conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                conversation.getSequence() / 1000,
                (int) ((conversation.getSequence() % 1000) * 1_000_000)
            ));
        }
        
        // Timestamp değeri yoksa, sequence değeriyle uyumlu bir timestamp oluştur
        if (conversation.getTimestamp() == null) {
            // Eğer sequence varsa, timestamp değerini onunla uyumlu hale getir
            if (conversation.getSequence() != null) {
                conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                    conversation.getSequence() / 1000,
                    (int) ((conversation.getSequence() % 1000) * 1_000_000)
                ));
            } else {
                conversation.setTimestamp(Timestamp.now());
            }
        }
        
        log.debug("Sohbete mesaj ekleniyor, Chat ID: {}, Sequence: {}", historyId, conversation.getSequence());
        return chatHistoryRepository.addConversation(historyId, conversation)
                .doOnSuccess(result -> log.info("Mesaj başarıyla eklendi, Chat ID: {}, Sequence: {}", 
                                          historyId, conversation.getSequence()))
                .doOnError(error -> log.error("Mesaj eklenirken hata, Chat ID {}, Sequence {}: {}", 
                                        historyId, conversation.getSequence(), error.getMessage()))
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
}
