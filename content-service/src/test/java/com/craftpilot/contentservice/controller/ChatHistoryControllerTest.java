package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.ChatHistory;
import com.craftpilot.contentservice.model.ChatMessage;
import com.craftpilot.contentservice.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ChatHistoryController.class)
class ChatHistoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
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
        when(chatHistoryService.createChatHistory(eq(userId), eq(contentId)))
                .thenReturn(Mono.just(chatHistory));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/chat-histories")
                        .queryParam("userId", userId)
                        .queryParam("contentId", contentId)
                        .build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChatHistory.class)
                .isEqualTo(chatHistory);
    }

    @Test
    void getChatHistory_Success() {
        when(chatHistoryService.getChatHistory(chatHistoryId))
                .thenReturn(Mono.just(chatHistory));

        webTestClient.get()
                .uri("/api/v1/chat-histories/{id}", chatHistoryId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatHistory.class)
                .isEqualTo(chatHistory);
    }

    @Test
    void getChatHistory_NotFound() {
        when(chatHistoryService.getChatHistory(chatHistoryId))
                .thenReturn(Mono.error(new RuntimeException("Chat history not found")));

        webTestClient.get()
                .uri("/api/v1/chat-histories/{id}", chatHistoryId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getChatHistoriesByUserId_Success() {
        when(chatHistoryService.getChatHistoriesByUserId(userId))
                .thenReturn(Flux.just(chatHistory));

        webTestClient.get()
                .uri("/api/v1/chat-histories/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatHistory.class)
                .hasSize(1)
                .contains(chatHistory);
    }

    @Test
    void addMessage_Success() {
        ChatHistory updatedChatHistory = chatHistory.toBuilder().build();
        updatedChatHistory.getMessages().add(chatMessage);

        when(chatHistoryService.addMessage(eq(chatHistoryId), any(ChatMessage.class)))
                .thenReturn(Mono.just(updatedChatHistory));

        webTestClient.post()
                .uri("/api/v1/chat-histories/{id}/messages", chatHistoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatMessage)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatHistory.class)
                .isEqualTo(updatedChatHistory);
    }

    @Test
    void deleteChatHistory_Success() {
        when(chatHistoryService.deleteChatHistory(chatHistoryId))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/chat-histories/{id}", chatHistoryId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteChatHistory_NotFound() {
        when(chatHistoryService.deleteChatHistory(chatHistoryId))
                .thenReturn(Mono.error(new RuntimeException("Chat history not found")));

        webTestClient.delete()
                .uri("/api/v1/chat-histories/{id}", chatHistoryId)
                .exchange()
                .expectStatus().is5xxServerError();
    }
} 