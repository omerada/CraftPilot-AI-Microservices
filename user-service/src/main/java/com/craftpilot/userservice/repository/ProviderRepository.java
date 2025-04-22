package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.Provider;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends ReactiveCrudRepository<Provider, String> {
}
