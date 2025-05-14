package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.model.ResponsePreference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ResponsePreferenceRepository extends ReactiveMongoRepository<ResponsePreference, String> {
    
    Mono<ResponsePreference> findByUserId(String userId);
    
    Mono<Void> deleteByUserId(String userId);
}
