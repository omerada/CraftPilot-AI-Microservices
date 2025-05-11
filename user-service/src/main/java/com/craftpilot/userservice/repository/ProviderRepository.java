package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProviderRepository extends ReactiveMongoRepository<Provider, String> {
    
    Flux<Provider> findByEnabled(boolean enabled);
    
    Mono<Provider> findByName(String name);
    
    @org.springframework.data.mongodb.repository.Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Flux<Provider> findByNameContainingIgnoreCase(String name);
}
