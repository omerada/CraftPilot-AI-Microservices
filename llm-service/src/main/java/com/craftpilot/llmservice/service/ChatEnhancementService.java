package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.client.UserMemoryClient;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.UserMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEnhancementService {
    private final UserMemoryClient userMemoryClient;
    private final UserInformationExtractionService extractionService;

    public Mono<AIRequest> enhanceRequestWithUserMemory(AIRequest request, String userId) {
        if (userId == null) {
            return Mono.just(request);
        }

        return userMemoryClient.getUserMemory(userId)
                .map(userMemory -> enhancePromptWithMemory(request, userMemory))
                .defaultIfEmpty(request);
    }

    public Mono<Void> processUserMessage(String userId, String message, String context) {
        if (userId == null) {
            log.debug("Skipping user message processing because userId is null");
            return Mono.empty();
        }
        
        log.info("Processing user message for memory extraction: userId={}, messageLength={}", 
                userId, message != null ? message.length() : 0);
        
        // Burada then() metodu kullanılarak Mono<Object> -> Mono<Void> dönüşümü yapılmalı
        return extractionService.processAndStoreUserInfo(userId, message)
                .doOnSubscribe(s -> log.debug("Started user message processing"))
                .doOnSuccess(v -> log.info("Completed user message processing for user {}", userId))
                .doOnError(e -> log.error("Error processing user message for user {}: {}", userId, e.getMessage()))
                .onErrorResume(e -> {
                    // Hatayı logla ama akışı kesme
                    log.error("Caught error in processUserMessage but continuing: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                // then() kullanarak akışı Mono<Void>'e dönüştür
                .then();
    }

    private AIRequest enhancePromptWithMemory(AIRequest request, UserMemory userMemory) {
        if (userMemory == null || userMemory.getMemory() == null || userMemory.getMemory().isEmpty()) {
            return request;
        }

        String memoryContext = "Seninle ilgili bildiklerim:\n" +
                userMemory.getMemory().stream()
                        .map(entry -> "- " + entry.getInformation())
                        .collect(Collectors.joining("\n"));

        String enhancedPrompt = memoryContext + "\n\n" + request.getPrompt();
        
        return AIRequest.builder()
                .model(request.getModel())
                .prompt(enhancedPrompt)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stream(Boolean.TRUE.equals(request.getStream()))
                .build();
    }
}
