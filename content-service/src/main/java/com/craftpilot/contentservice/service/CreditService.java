package com.craftpilot.contentservice.service;
 
import com.craftpilot.contentservice.model.Credit;
import com.craftpilot.contentservice.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;
    private static final int DEFAULT_INITIAL_CREDITS = 100;

    public Mono<Credit> createCredit(String userId) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    Credit credit = Credit.builder()
                            .userId(userId)
                            .credits(DEFAULT_INITIAL_CREDITS)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return creditRepository.save(credit);
                }));
    }

    public Mono<Credit> getCredit(String userId) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found for user: " + userId)));
    }

    public Mono<Credit> updateCredits(String userId, int credits) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found for user: " + userId)))
                .flatMap(existingCredit -> creditRepository.updateCredits(userId, credits));
    }

    public Mono<Credit> deductCredits(String userId, int creditsToDeduct) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found for user: " + userId)))
                .flatMap(existingCredit -> {
                    if (existingCredit.getCredits() < creditsToDeduct) {
                        return Mono.error(new RuntimeException("Insufficient credits"));
                    }
                    return creditRepository.updateCredits(userId, existingCredit.getCredits() - creditsToDeduct);
                });
    }

    public Mono<Credit> addCredits(String userId, int creditsToAdd) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found for user: " + userId)))
                .flatMap(existingCredit -> 
                    creditRepository.updateCredits(userId, existingCredit.getCredits() + creditsToAdd));
    }

    public Mono<Void> deleteCredit(String userId) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found for user: " + userId)))
                .flatMap(credit -> creditRepository.deleteByUserId(userId));
    }
} 