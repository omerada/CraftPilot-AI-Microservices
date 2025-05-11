package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserPreferenceRepository extends ReactiveMongoRepository<UserPreference, String> {
    
    Mono<UserPreference> findByUserId(String userId);
    
    Mono<Void> deleteByUserId(String userId);
}
