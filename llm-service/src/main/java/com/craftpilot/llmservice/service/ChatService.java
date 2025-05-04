package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.ChatRequest;
import com.craftpilot.llmservice.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final LLMService llmService;
    private final ChatEnhancementService enhancementService;

    public Mono<ChatResponse> processChat(ChatRequest chatRequest) {
        String userId = chatRequest.getUserId();
        String message = chatRequest.getMessage();
        String context = chatRequest.getContext();

        // Chat isteğinden AI isteği oluştur
        AIRequest aiRequest = AIRequest.builder()
                .model(chatRequest.getModel())
                .prompt(message)
                .temperature(chatRequest.getTemperature())
                .maxTokens(chatRequest.getMaxTokens())
                .stream(Boolean.TRUE)
                .build();

        // Bilgi çıkarımı için mesajı işle (asenkron)
        enhancementService.processUserMessage(userId, message, context)
                .subscribe();

        // İsteği kullanıcı belleği ile zenginleştir
        return enhancementService.enhanceRequestWithUserMemory(aiRequest, userId)
                .flatMap(llmService::processChatCompletion)
                .map(aiResponse -> mapToChatResponse(aiResponse, chatRequest));
    }

    private ChatResponse mapToChatResponse(AIResponse aiResponse, ChatRequest request) {
        Integer tokensUsed = aiResponse.getTokensUsed();
        return ChatResponse.builder()
                .userId(request.getUserId())
                .message(request.getMessage())
                .response(aiResponse.getResponse())
                .model(aiResponse.getModel())
                .processedTokens(tokensUsed)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
