package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.repository.UserMemoryRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMemoryService {

    private final UserMemoryRepository userMemoryRepository;

    public Mono<UserMemory> getUserMemory(String userId) {
        return userMemoryRepository.findByUserId(userId);
    }

    public Mono<UserMemory> addMemoryEntry(String userId, UserMemory.MemoryEntry entry) {
        log.info("Adding memory entry for userId: {}", userId);
        
        // Generate ID if not present
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        
        // Set timestamp if not present
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(Timestamp.now());
        }
        
        return userMemoryRepository.findByUserId(userId)
                .defaultIfEmpty(createNewUserMemory(userId))
                .flatMap(userMemory -> {
                    if (userMemory.getEntries() == null) {
                        userMemory.setEntries(new ArrayList<>());
                    }
                    
                    userMemory.getEntries().add(entry);
                    userMemory.setUpdatedAt(Timestamp.now());
                    
                    log.info("Saving user memory with new entry, total entries: {}", 
                            userMemory.getEntries().size());
                    return userMemoryRepository.save(userMemory);
                });
    }

    public Mono<UserMemory> updateUserMemory(UserMemory userMemory) {
        log.info("Updating user memory for userId: {}", userMemory.getUserId());
        userMemory.setUpdatedAt(Timestamp.now());
        return userMemoryRepository.save(userMemory);
    }

    public Mono<UserMemory> cleanOldEntries(String userId, int daysThreshold) {
        log.info("Cleaning entries older than {} days for userId: {}", daysThreshold, userId);
        
        Instant thresholdInstant = Instant.now().minus(daysThreshold, ChronoUnit.DAYS);
        Timestamp thresholdTimestamp = Timestamp.ofTimeSecondsAndNanos(
                thresholdInstant.getEpochSecond(), 
                thresholdInstant.getNano());
        
        return userMemoryRepository.findByUserId(userId)
                .flatMap(userMemory -> {
                    if (userMemory.getEntries() == null || userMemory.getEntries().isEmpty()) {
                        log.info("No entries to clean for userId: {}", userId);
                        return Mono.just(userMemory);
                    }
                    
                    int originalSize = userMemory.getEntries().size();
                    
                    userMemory.setEntries(
                            userMemory.getEntries().stream()
                                    .filter(entry -> entry.getTimestamp().compareTo(thresholdTimestamp) > 0)
                                    .collect(Collectors.toList())
                    );
                    
                    int newSize = userMemory.getEntries().size();
                    int removedEntries = originalSize - newSize;
                    
                    log.info("Cleaned {} old entries for userId: {}", removedEntries, userId);
                    
                    if (removedEntries > 0) {
                        userMemory.setUpdatedAt(Timestamp.now());
                        return userMemoryRepository.save(userMemory);
                    } else {
                        return Mono.just(userMemory);
                    }
                });
    }
    
    private UserMemory createNewUserMemory(String userId) {
        log.info("Creating new user memory for userId: {}", userId);
        Timestamp now = Timestamp.now();
        return UserMemory.builder()
                .userId(userId)
                .id(userId)
                .createdAt(now)
                .updatedAt(now)
                .entries(new ArrayList<>())
                .build();
    }
}
