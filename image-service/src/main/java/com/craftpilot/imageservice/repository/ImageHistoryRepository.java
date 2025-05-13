package com.craftpilot.imageservice.repository;

import com.craftpilot.imageservice.model.ImageHistory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ImageHistoryRepository extends ReactiveMongoRepository<ImageHistory, String> {
    
    Flux<ImageHistory> findByUserId(String userId);
}