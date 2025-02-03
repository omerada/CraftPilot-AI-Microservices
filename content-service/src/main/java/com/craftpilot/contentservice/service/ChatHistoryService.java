package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.exception.ChatHistoryNotFoundException;
import com.craftpilot.contentservice.model.ChatHistory;
import com.craftpilot.contentservice.model.ChatMessage;
import com.craftpilot.contentservice.repository.ChatHistoryRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<ChatHistory> createChatHistory(String userId, String contentId) {
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .contentId(contentId)
                .messages(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return chatHistoryRepository.save(chatHistory)
                .doOnSuccess(saved -> log.info("Chat history created successfully: {}", saved.getId()));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<ChatHistory> getChatHistory(String id) {
        return chatHistoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ChatHistoryNotFoundException("Chat history not found with id: " + id)))
                .doOnSuccess(history -> log.info("Chat history retrieved successfully: {}", id));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Flux<ChatHistory> getChatHistoriesByUserId(String userId) {
        return chatHistoryRepository.findByUserId(userId)
                .doOnComplete(() -> log.info("Chat histories retrieved successfully for user: {}", userId));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<ChatHistory> addMessage(String id, ChatMessage message) {
        return getChatHistory(id)
                .map(chatHistory -> {
                    chatHistory.getMessages().add(message);
                    chatHistory.setUpdatedAt(Instant.now());
                    return chatHistory;
                })
                .flatMap(chatHistoryRepository::save);
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<Void> deleteChatHistory(String id) {
        return chatHistoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ChatHistoryNotFoundException("Chat history not found with id: " + id)))
                .flatMap(chatHistory -> chatHistoryRepository.deleteById(id))
                .doOnSuccess(v -> log.info("Chat history deleted successfully: {}", id));
    }
} 