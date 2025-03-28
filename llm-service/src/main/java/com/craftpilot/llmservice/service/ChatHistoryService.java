package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;

    public Flux<ChatHistory> getChatHistoriesByUserId(String userId) {
        return chatHistoryRepository.findAllByUserId(userId);
    }

    public Mono<ChatHistory> getChatHistoryById(String id) {
        return chatHistoryRepository.findById(id);
    }

    public Mono<ChatHistory> createChatHistory(ChatHistory chatHistory) {
        // ID yoksa oluştur
        if (chatHistory.getId() == null) {
            chatHistory.setId(UUID.randomUUID().toString());
        }
        return chatHistoryRepository.save(chatHistory);
    }

    public Mono<ChatHistory> updateChatHistory(ChatHistory chatHistory) {
        return chatHistoryRepository.save(chatHistory);
    }

    public Mono<Void> deleteChatHistory(String id) {
        return chatHistoryRepository.delete(id);
    }

    public Mono<ChatHistory> addConversation(String historyId, Conversation conversation) {
        // Conversation ID yoksa oluştur
        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID().toString());
        }
        return chatHistoryRepository.addConversation(historyId, conversation);
    }

    public Mono<ChatHistory> updateChatHistoryTitle(String historyId, String newTitle) {
        return chatHistoryRepository.updateTitle(historyId, newTitle);
    }
}
