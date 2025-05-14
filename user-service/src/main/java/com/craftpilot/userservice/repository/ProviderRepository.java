package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProviderRepository extends ReactiveMongoRepository<Provider, String> {
    // Provider'ları bulmak için ek metotlar buraya eklenebilir
}
