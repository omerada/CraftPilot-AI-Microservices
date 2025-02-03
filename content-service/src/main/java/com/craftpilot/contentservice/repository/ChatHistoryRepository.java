package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.ChatHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatHistoryRepository {
    Mono<ChatHistory> save(ChatHistory chatHistory);
    Mono<ChatHistory> findById(String id);
    Flux<ChatHistory> findByUserId(String userId);
    Flux<ChatHistory> findByContentId(String contentId);
    Mono<Void> deleteById(String id);
} 