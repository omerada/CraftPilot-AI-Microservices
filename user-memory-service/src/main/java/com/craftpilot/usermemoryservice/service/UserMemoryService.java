package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.model.MemoryItem;
import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.model.dto.ExtractionRequest;
import com.craftpilot.usermemoryservice.model.dto.ExtractionResponse;
import com.craftpilot.usermemoryservice.model.dto.InformationItem;
import com.craftpilot.usermemoryservice.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMemoryService {
    
    private final UserMemoryRepository userMemoryRepository;
    private final WebClient webClient;
    
    @Value("${llm-service.url}")
    private String llmServiceUrl;
    
    @Value("${user-memory.cleanup.days:30}")
    private int cleanupDays;
    
    public Mono<UserMemory> getUserMemory(String userId) {
        log.info("Fetching memory for user: {}", userId);
        return userMemoryRepository.findByUserId(userId)
            .switchIfEmpty(Mono.defer(() -> {
                log.info("No existing memory found for user: {}. Creating new memory.", userId);
                return createNewUserMemory(userId);
            }));
    }
    
    public Mono<Void> processUserMessage(String userId, String message) {
        log.info("Processing user message for memory extraction: {}", userId);
        return extractAndSaveMemory(userId, message).then();
    }
    
    public Mono<UserMemory> extractAndSaveMemory(String userId, String message) {
        log.info("Extracting information from message for user: {}", userId);
        
        if (message == null || message.trim().length() < 10) {
            log.info("Message too short, skipping extraction for user: {}", userId);
            return getUserMemory(userId);
        }
        
        ExtractionRequest request = ExtractionRequest.builder()
                .userId(userId)
                .message(message)
                .build();
        
        return webClient.post()
                .uri(llmServiceUrl + "/extract")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ExtractionResponse.class)
                .flatMap(response -> {
                    log.info("Received extraction response for user: {}", userId);
                    
                    if (response.getInformationItems() == null || response.getInformationItems().isEmpty()) {
                        log.info("No information extracted for user: {}", userId);
                        return getUserMemory(userId);
                    }
                    
                    return getUserMemory(userId)
                            .flatMap(userMemory -> {
                                for (InformationItem item : response.getInformationItems()) {
                                    MemoryItem memoryItem = MemoryItem.builder()
                                            .id(UUID.randomUUID().toString())
                                            .content(item.getContent())
                                            .category(item.getCategory())
                                            .importance(item.getImportance())
                                            .source("message-extraction")
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    
                                    userMemory.getMemories().add(memoryItem);
                                }
                                
                                userMemory.setUpdatedAt(LocalDateTime.now());
                                return userMemoryRepository.save(userMemory);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error during information extraction: {}", e.getMessage(), e);
                    return getUserMemory(userId);
                });
    }
    
    private Mono<UserMemory> createNewUserMemory(String userId) {
        log.info("Creating new user memory for user: {}", userId);
        
        UserMemory newUserMemory = UserMemory.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .memories(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();
        
        return userMemoryRepository.save(newUserMemory);
    }
}
