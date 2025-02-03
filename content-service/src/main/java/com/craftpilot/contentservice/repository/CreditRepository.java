package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Credit;
import reactor.core.publisher.Mono;

public interface CreditRepository {
    Mono<Credit> save(Credit credit);
    Mono<Credit> findById(String id);
    Mono<Credit> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Mono<Credit> updateCredits(String userId, int credits);
    Mono<Void> deleteByUserId(String userId);
} 