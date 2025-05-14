package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.dto.MemoryEntryRequest;
import com.craftpilot.usermemoryservice.dto.MemoryResponse;
import com.craftpilot.usermemoryservice.exception.MongoDBException;
import com.craftpilot.usermemoryservice.model.MemoryItem;
import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.repository.UserMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserMemoryService {
    
    private final UserMemoryRepository userMemoryRepository;
    
    public UserMemoryService(UserMemoryRepository userMemoryRepository) {
        this.userMemoryRepository = userMemoryRepository;
    }
    
    public Mono<UserMemory> getUserMemory(String userId) {
        log.info("Fetching user memory for userId: {}", userId);
        
        return userMemoryRepository.findByUserId(userId)
            .switchIfEmpty(
                Mono.defer(() -> {
                    log.info("No existing user memory found for userId: {}, creating new one", userId);
                    UserMemory newMemory = UserMemory.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .created(LocalDateTime.now())
                        .lastUpdated(LocalDateTime.now())
                        .entries(new ArrayList<>())
                        .build();
                    return userMemoryRepository.save(newMemory);
                })
            )
            .onErrorMap(e -> {
                log.error("Error retrieving user memory: {}", e.getMessage(), e);
                return new MongoDBException("Kullanıcı belleği alınırken bir hata oluştu: " + e.getMessage(), e);
            });
    }
    
    public Mono<MemoryResponse> addMemoryEntry(String userId, MemoryEntryRequest request) {
        log.info("Adding memory entry for userId: {}", userId);
        
        // Girdi için tarih ve önem derecesi kontrolü
        if (request.getTimestamp() == null) {
            request.setTimestamp(LocalDateTime.now());
        }
        
        if (request.getImportance() == null) {
            request.setImportance(1.0);
        }
        
        MemoryItem newEntry = MemoryItem.builder()
            .content(request.getContent())
            .source(request.getSource())
            .context(request.getContext())
            .metadata(request.getMetadata())
            .timestamp(request.getTimestamp())
            .importance(request.getImportance())
            .build();
        
        return getUserMemory(userId)
            .flatMap(memory -> {
                // Yeni girdiyi ekleyelim
                if (memory.getEntries() == null) {
                    memory.setEntries(new ArrayList<>());
                }
                memory.getEntries().add(newEntry);
                memory.setLastUpdated(LocalDateTime.now());
                
                return userMemoryRepository.save(memory);
            })
            .map(savedMemory -> new MemoryResponse("Bellek girdisi başarıyla eklendi", true))
            .onErrorMap(e -> {
                log.error("Error adding memory entry: {}", e.getMessage(), e);
                return new MongoDBException("Bellek girdisi eklenirken bir hata oluştu: " + e.getMessage(), e);
            });
    }
    
    public Mono<MemoryResponse> deleteMemoryEntry(String userId, int entryIndex) {
        log.info("Deleting memory entry at index {} for userId: {}", entryIndex, userId);
        
        return getUserMemory(userId)
            .flatMap(memory -> {
                List<MemoryItem> entries = memory.getEntries();
                
                if (entries == null || entries.isEmpty()) {
                    return Mono.error(new IllegalArgumentException("Kullanıcının bellek girdisi bulunmamaktadır"));
                }
                
                if (entryIndex < 0 || entryIndex >= entries.size()) {
                    return Mono.error(new IndexOutOfBoundsException("Belirtilen indeks geçersiz: " + entryIndex));
                }
                
                // Belirtilen indeksteki girdiyi kaldır
                entries.remove(entryIndex);
                memory.setLastUpdated(LocalDateTime.now());
                
                return userMemoryRepository.save(memory);
            })
            .map(savedMemory -> new MemoryResponse("Bellek girdisi başarıyla silindi", true))
            .onErrorMap(e -> {
                if (e instanceof IndexOutOfBoundsException) {
                    return e;
                }
                log.error("Error deleting memory entry: {}", e.getMessage(), e);
                return new MongoDBException("Bellek girdisi silinirken bir hata oluştu: " + e.getMessage(), e);
            });
    }
    
    public Mono<MemoryResponse> deleteAllMemoryEntries(String userId) {
        log.info("Deleting all memory entries for userId: {}", userId);
        
        return getUserMemory(userId)
            .flatMap(memory -> {
                memory.setEntries(new ArrayList<>());
                memory.setLastUpdated(LocalDateTime.now());
                return userMemoryRepository.save(memory);
            })
            .map(savedMemory -> new MemoryResponse("Tüm bellek girdileri başarıyla silindi", true))
            .onErrorMap(e -> {
                log.error("Error deleting all memory entries: {}", e.getMessage(), e);
                return new MongoDBException("Tüm bellek girdileri silinirken bir hata oluştu: " + e.getMessage(), e);
            });
    }
    
    // Add this new method to fix the reference in the controller
    public Mono<Void> deleteAllMemories(String userId) {
        log.info("Deleting all memories for userId: {}", userId);
        
        return getUserMemory(userId)
            .flatMap(memory -> {
                memory.setEntries(new ArrayList<>());
                memory.setLastUpdated(LocalDateTime.now());
                return userMemoryRepository.save(memory);
            })
            .then();
    }
}
