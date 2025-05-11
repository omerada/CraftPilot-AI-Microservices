package com.craftpilot.creditservice.repository;

import com.craftpilot.creditservice.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {
    Mono<Credit> findByUserId(String userId);
    Mono<Credit> findByUserIdAndDeletedFalse(String userId);
}