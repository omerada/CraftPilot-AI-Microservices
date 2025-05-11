package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.AIModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AIModelRepository extends ReactiveMongoRepository<AIModel, String> {
    
    Flux<AIModel> findByProviderId(String providerId);
    
    Flux<AIModel> findByEnabled(boolean enabled);
    
    @Query("{ 'providerId': ?0, 'enabled': true }")
    Flux<AIModel> findEnabledByProviderId(String providerId);
    
    Mono<AIModel> findByModelId(String modelId);
    
    @Query("{ 'tags': { $in: ?0 } }")
    Flux<AIModel> findByTagsIn(String[] tags);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Flux<AIModel> findByNameContainingIgnoreCase(String name);
}
