package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ProviderRepository extends ReactiveMongoRepository<Provider, String> {
    Mono<Provider> findByName(String name);
}
