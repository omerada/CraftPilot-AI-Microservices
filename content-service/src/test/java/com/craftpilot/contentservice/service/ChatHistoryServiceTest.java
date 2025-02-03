package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.exception.ChatHistoryNotFoundException;
import com.craftpilot.contentservice.model.ChatHistory;
import com.craftpilot.contentservice.model.ChatMessage;
import com.craftpilot.contentservice.repository.ChatHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    private ChatHistory chatHistory;
    private ChatMessage chatMessage;
    private final String userId = "test-user";
    private final String contentId = "test-content";
    private final String chatHistoryId = "test-chat-history";

    @BeforeEach
    void setUp() {
        chatMessage = ChatMessage.builder()
                .role("user")
                .content("Test message")
                .timestamp(Instant.now())
                .build();

        chatHistory = ChatHistory.builder()
                .id(chatHistoryId)
                .userId(userId)
                .contentId(contentId)
                .messages(new ArrayList<>())
                .metadata(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createChatHistory_Success() {
        when(chatHistoryRepository.save(any(ChatHistory.class)))
                .thenReturn(Mono.just(chatHistory));

        StepVerifier.create(chatHistoryService.createChatHistory(userId, contentId))
                .expectNext(chatHistory)
                .verifyComplete();
    }

    @Test
    void getChatHistory_Success() {
        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.just(chatHistory));

        StepVerifier.create(chatHistoryService.getChatHistory(chatHistoryId))
                .expectNext(chatHistory)
                .verifyComplete();
    }

    @Test
    void getChatHistory_NotFound() {
        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatHistoryService.getChatHistory(chatHistoryId))
                .expectError(ChatHistoryNotFoundException.class)
                .verify();
    }

    @Test
    void getChatHistoriesByUserId_Success() {
        when(chatHistoryRepository.findByUserId(userId))
                .thenReturn(Flux.just(chatHistory));

        StepVerifier.create(chatHistoryService.getChatHistoriesByUserId(userId))
                .expectNext(chatHistory)
                .verifyComplete();
    }

    @Test
    void addMessage_Success() {
        ChatHistory updatedChatHistory = chatHistory.toBuilder().build();
        updatedChatHistory.getMessages().add(chatMessage);

        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.just(chatHistory));
        when(chatHistoryRepository.save(any(ChatHistory.class)))
                .thenReturn(Mono.just(updatedChatHistory));

        StepVerifier.create(chatHistoryService.addMessage(chatHistoryId, chatMessage))
                .expectNext(updatedChatHistory)
                .verifyComplete();
    }

    @Test
    void addMessage_NotFound() {
        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatHistoryService.addMessage(chatHistoryId, chatMessage))
                .expectError(ChatHistoryNotFoundException.class)
                .verify();
    }

    @Test
    void deleteChatHistory_Success() {
        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.just(chatHistory));
        when(chatHistoryRepository.deleteById(chatHistoryId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatHistoryService.deleteChatHistory(chatHistoryId))
                .verifyComplete();
    }

    @Test
    void deleteChatHistory_NotFound() {
        when(chatHistoryRepository.findById(chatHistoryId))
                .thenReturn(Mono.empty());

        StepVerifier.create(chatHistoryService.deleteChatHistory(chatHistoryId))
                .expectError(ChatHistoryNotFoundException.class)
                .verify();
    }
} 