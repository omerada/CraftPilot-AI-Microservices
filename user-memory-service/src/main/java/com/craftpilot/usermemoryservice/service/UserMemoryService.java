package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMemoryService {
    private final UserMemoryRepository userMemoryRepository;

    public Mono<UserMemory> getUserMemory(String userId) {
        return userMemoryRepository.findByUserId(userId)
                .switchIfEmpty(createEmptyUserMemory(userId))
                .doOnSuccess(memory -> log.info("Retrieved user memory for user: {}", userId))
                .doOnError(e -> log.error("Error retrieving user memory for user: {}", userId, e));
    }

    public Mono<UserMemory> addMemoryEntry(String userId, UserMemory.MemoryEntry entry) {
        return getUserMemory(userId)
                .flatMap(userMemory -> {
                    userMemory.getMemory().add(entry);
                    userMemory.setLastUpdated(Instant.now());
                    return userMemoryRepository.save(userMemory);
                })
                .doOnSuccess(memory -> log.info("Added memory entry for user: {}", userId))
                .doOnError(e -> log.error("Error adding memory entry for user: {}", userId, e));
    }

    public Mono<UserMemory> updateUserMemory(UserMemory userMemory) {
        userMemory.setLastUpdated(Instant.now());
        return userMemoryRepository.save(userMemory)
                .doOnSuccess(memory -> log.info("Updated user memory for user: {}", userMemory.getUserId()))
                .doOnError(e -> log.error("Error updating user memory for user: {}", userMemory.getUserId(), e));
    }

    public Mono<UserMemory> cleanOldEntries(String userId, int daysThreshold) {
        return getUserMemory(userId)
                .flatMap(userMemory -> {
                    Instant threshold = Instant.now().minus(daysThreshold, ChronoUnit.DAYS);
                    List<UserMemory.MemoryEntry> validEntries = userMemory.getMemory().stream()
                            .filter(entry -> entry.getTimestamp().isAfter(threshold))
                            .collect(Collectors.toList());
                    
                    userMemory.setMemory(validEntries);
                    userMemory.setLastUpdated(Instant.now());
                    
                    return userMemoryRepository.save(userMemory);
                })
                .doOnSuccess(memory -> log.info("Cleaned old entries for user: {}", userId))
                .doOnError(e -> log.error("Error cleaning old entries for user: {}", userId, e));
    }

    private Mono<UserMemory> createEmptyUserMemory(String userId) {
        UserMemory userMemory = UserMemory.builder()
                .userId(userId)
                .memory(new ArrayList<>())
                .lastUpdated(Instant.now())
                .build();
        
        return userMemoryRepository.save(userMemory);
    }
}
