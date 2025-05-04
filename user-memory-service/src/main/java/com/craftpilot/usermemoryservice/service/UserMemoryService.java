package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.dto.MemoryEntryRequest;
import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMemoryService {
    private final UserMemoryRepository userMemoryRepository;

    public Mono<UserMemory> addMemoryEntry(String userId, MemoryEntryRequest request) {
        log.info("Processing extracted information for userId: {}", userId);
        
        return userMemoryRepository.findByUserId(userId)
                .defaultIfEmpty(createNewUserMemory(userId))
                .flatMap(userMemory -> {
                    log.info("Adding memory entry for userId: {}", userId);
                    
                    // Memory entry'yi ekleyelim
                    if (userMemory.getEntries() == null) {
                        userMemory.setEntries(new ArrayList<>());
                    }
                    
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("content", request.getContent());
                    entry.put("source", request.getSource());
                    entry.put("context", request.getContext());
                    entry.put("metadata", request.getMetadata());
                    entry.put("timestamp", request.getTimestamp() != null 
                            ? request.getTimestamp() : LocalDateTime.now());
                    entry.put("importance", request.getImportance() != null 
                            ? request.getImportance() : 1.0);
                    
                    userMemory.getEntries().add(entry);
                    
                    // Son güncelleme zamanını ayarlayalım
                    userMemory.setLastUpdated(LocalDateTime.now());
                    
                    return userMemoryRepository.save(userMemory)
                            .thenReturn(userMemory);
                });
    }

    public Mono<UserMemory> getUserMemory(String userId) {
        return userMemoryRepository.findByUserId(userId);
    }

    private UserMemory createNewUserMemory(String userId) {
        log.info("Creating new user memory for userId: {}", userId);
        
        UserMemory userMemory = new UserMemory();
        userMemory.setUserId(userId);
        userMemory.setEntries(new ArrayList<>());
        userMemory.setCreated(LocalDateTime.now());
        userMemory.setLastUpdated(LocalDateTime.now());
        return userMemory;
    }
}
