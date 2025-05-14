package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ChatHistoryRepository extends ReactiveMongoRepository<ChatHistory, String> {
    Flux<ChatHistory> findByUserId(String userId);

    Flux<ChatHistory> findBySessionId(String sessionId);

    @Query("{ 'userId': ?0, 'timestamp': { $gte: ?1 } }")
    Flux<ChatHistory> findByUserIdAndTimestampAfter(String userId, LocalDateTime timestamp);

    @Query(value = "{ 'userId': ?0 }", sort = "{ 'updatedAt': -1 }")
    Flux<ChatHistory> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    Mono<Void> deleteBySessionId(String sessionId);

    // Aşağıdaki metotlar implementation sınıfında oluşturulmalı
    default Flux<ChatHistory> findAllByUserId(String userId, int page, int pageSize) {
        return findByUserIdOrderByUpdatedAtDesc(userId, Pageable.ofSize(pageSize).withPage(page));
    }

    Mono<ChatHistory> addConversation(String historyId, Conversation conversation);

    Mono<ChatHistory> updateTitle(String historyId, String newTitle);

    Mono<Void> delete(String id);
}
