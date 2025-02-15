package com.craftpilot.creditservice.service;

import com.craftpilot.creditservice.event.CreditEvent;
import com.craftpilot.creditservice.exception.InsufficientCreditsException;
import com.craftpilot.creditservice.model.Credit;
import com.craftpilot.creditservice.model.CreditTransaction;
import com.craftpilot.creditservice.repository.CreditRepository;
import com.craftpilot.creditservice.repository.CreditTransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final KafkaTemplate<String, CreditEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topics.credit-events}")
    private String creditEventsTopic;

    public Mono<Credit> getUserCredits(String userId) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(createInitialCredit(userId));
    }

    public Mono<CreditTransaction> processTransaction(String userId, String serviceId, BigDecimal amount, 
            CreditTransaction.TransactionType type, String description) {
        return getUserCredits(userId)
                .flatMap(credit -> {
                    if (type == CreditTransaction.TransactionType.DEBIT && credit.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new InsufficientCreditsException("Insufficient credits"));
                    }

                    CreditTransaction transaction = CreditTransaction.builder()
                            .userId(userId)
                            .serviceId(serviceId)
                            .type(type)
                            .amount(amount)
                            .description(description)
                            .status(CreditTransaction.TransactionStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return transactionRepository.save(transaction)
                            .flatMap(savedTransaction -> updateCreditBalance(credit, savedTransaction)
                                    .flatMap(updatedCredit -> {
                                        savedTransaction.setStatus(CreditTransaction.TransactionStatus.COMPLETED);
                                        return transactionRepository.save(savedTransaction);
                                    }));
                })
                .doOnSuccess(transaction -> {
                    meterRegistry.counter("credit.transactions", "type", transaction.getType().toString()).increment();
                    kafkaTemplate.send(creditEventsTopic, userId, new CreditEvent(transaction))
                        .addCallback(
                            result -> log.debug("Credit event sent successfully for user: {}", userId),
                            ex -> log.error("Failed to send credit event for user: {}", userId, ex)
                        );
                })
                .doOnError(error -> log.error("Error processing credit transaction", error));
    }

    public Flux<CreditTransaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    private Mono<Credit> createInitialCredit(String userId) {
        Credit credit = Credit.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .totalCreditsEarned(BigDecimal.ZERO)
                .totalCreditsUsed(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
        return creditRepository.save(credit);
    }

    private Mono<Credit> updateCreditBalance(Credit credit, CreditTransaction transaction) {
        BigDecimal newBalance = transaction.getType() == CreditTransaction.TransactionType.CREDIT
                ? credit.getBalance().add(transaction.getAmount())
                : credit.getBalance().subtract(transaction.getAmount());

        credit.setBalance(newBalance);
        credit.setLastUpdated(LocalDateTime.now());

        if (transaction.getType() == CreditTransaction.TransactionType.CREDIT) {
            credit.setTotalCreditsEarned(credit.getTotalCreditsEarned().add(transaction.getAmount()));
        } else {
            credit.setTotalCreditsUsed(credit.getTotalCreditsUsed().add(transaction.getAmount()));
        }

        return creditRepository.save(credit);
    }
}