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
            return Mono.empty();
        }
        
        return extractionService.processAndStoreUserInfo(userId, message, context);
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
