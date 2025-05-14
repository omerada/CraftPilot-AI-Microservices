package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.model.UserMemory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserMemoryRepository extends ReactiveMongoRepository<UserMemory, String> {
    
    Mono<UserMemory> findByUserId(String userId);
    
    Mono<Void> deleteByUserId(String userId);
}
