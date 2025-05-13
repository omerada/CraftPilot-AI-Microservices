package com.craftpilot.creditservice.service;

import com.craftpilot.creditservice.event.CreditEvent;
import com.craftpilot.creditservice.exception.InsufficientCreditsException;
import com.craftpilot.creditservice.model.Credit;
import com.craftpilot.creditservice.model.CreditTransaction;
import com.craftpilot.creditservice.model.CreditType;
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
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final KafkaTemplate<String, CreditEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topics.credit-events:credit-events}")
    private String creditEventsTopic;

    @Value("${initial.credit.amount:100}")  // Varsayılan değer: 100
    private String initialCreditAmount;

    @Value("${initial.advanced.credit.amount:0}")  // Varsayılan değer: 0
    private String initialAdvancedCreditAmount;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    public Mono<Credit> getUserCredits(String userId) {
        return creditRepository.findByUserId(userId)
                .switchIfEmpty(createInitialCredit(userId));
    }

    public Mono<CreditTransaction> processTransaction(String userId, String serviceId, BigDecimal amount, 
            CreditTransaction.TransactionType type, String description) {
        return getUserCredits(userId)
                .flatMap(credit -> {
                    // String tipini TransactionType enum'a dönüştür
                    boolean isDebit = type == CreditTransaction.TransactionType.DEBIT;
                    
                    if (isDebit && credit.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new InsufficientCreditsException("Insufficient credits"));
                    }

                    CreditTransaction transaction = CreditTransaction.builder()
                            .userId(userId)
                            .serviceId(serviceId)
                            .type(isDebit ? "DEBIT" : "CREDIT") // String olarak ayarla
                            .type2(type) // Enum değerini yeni alana kaydet
                            .amount(amount)
                            .description(description)
                            .status(CreditTransaction.TransactionStatus.PENDING)
                            .timestamp(LocalDateTime.now())
                            .build();

                    return transactionRepository.save(transaction)
                            .flatMap(savedTransaction -> updateCreditBalance(credit, savedTransaction)
                                    .flatMap(updatedCredit -> {
                                        savedTransaction.setStatus(CreditTransaction.TransactionStatus.COMPLETED);
                                        return transactionRepository.save(savedTransaction);
                                    }));
                })
                .doOnSuccess(transaction -> {
                    meterRegistry.counter("credit.transactions", "type", transaction.getType()).increment();
                    CreditEvent event = CreditEvent.builder()
                            .userId(userId)
                            .amount(transaction.getAmount())
                            .type(transaction.getType())
                            .creditType(transaction.getCreditType())
                            .timestamp(System.currentTimeMillis())
                            .build();
                    kafkaTemplate.send(creditEventsTopic, userId, event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send credit event for user: {}", userId, ex);
                            } else {
                                log.debug("Credit event sent successfully for user: {}", userId);
                            }
                        });
                })
                .doOnError(error -> log.error("Error processing credit transaction", error));
    }

    /**
     * Yeni bir kredi işlemi yapar
     */
    public Mono<CreditTransaction> processTransaction(
            String userId, 
            String serviceId,
            BigDecimal amount,
            String type,
            String description,
            String creditType) {
        
        return getUserCredits(userId)
                .flatMap(credit -> {
                    // Kredi tipine göre bakiyeyi kontrol et
                    if ("ADVANCED".equals(creditType)) {
                        if (type.equals("DEBIT") && credit.getAdvancedBalance().compareTo(amount) < 0) {
                            return Mono.error(new InsufficientCreditsException("Yetersiz gelişmiş kredi bakiyesi"));
                        }
                    } else {
                        if (type.equals("DEBIT") && credit.getBalance().compareTo(amount) < 0) {
                            return Mono.error(new InsufficientCreditsException("Yetersiz kredi bakiyesi"));
                        }
                    }

                    // İşlem kaydı oluştur
                    CreditTransaction transaction = CreditTransaction.builder()
                            .userId(userId)
                            .serviceId(serviceId)
                            .amount(amount)
                            .type(type)
                            .description(description)
                            .creditType(creditType)
                            .timestamp(LocalDateTime.now())
                            .build();

                    // Krediyi güncelle
                    if ("ADVANCED".equals(creditType)) {
                        if (type.equals("DEBIT")) {
                            credit.setAdvancedBalance(credit.getAdvancedBalance().subtract(amount));
                            credit.setTotalAdvancedCreditsUsed(credit.getTotalAdvancedCreditsUsed().add(amount));
                        } else {
                            credit.setAdvancedBalance(credit.getAdvancedBalance().add(amount));
                            credit.setTotalAdvancedCreditsEarned(credit.getTotalAdvancedCreditsEarned().add(amount));
                        }
                    } else {
                        if (type.equals("DEBIT")) {
                            credit.setBalance(credit.getBalance().subtract(amount));
                            credit.setTotalCreditsUsed(credit.getTotalCreditsUsed().add(amount));
                        } else {
                            credit.setBalance(credit.getBalance().add(amount));
                            credit.setTotalCreditsEarned(credit.getTotalCreditsEarned().add(amount));
                        }
                    }

                    credit.setLastUpdated(LocalDateTime.now());

                    // Kredi ve işlemi kaydet
                    return creditRepository.save(credit)
                            .then(transactionRepository.save(transaction))
                            .doOnSuccess(t -> {
                                // Metrik ve olay gönderimi
                                publishCreditEvent(userId, amount, type, creditType);
                                recordCreditMetrics(userId, amount, type, creditType);
                            });
                });
    }

    public Flux<CreditTransaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserIdAndDeletedFalse(userId);
    }

    /**
     * Yeni kullanıcı için başlangıç kredisi oluşturur
     */
    private Mono<Credit> createInitialCredit(String userId) {
        log.info("Yeni kullanıcı için başlangıç kredisi oluşturuluyor: userId={}", userId);
        LocalDateTime now = LocalDateTime.now();
        
        Credit credit = Credit.builder()
                .userId(userId)
                .balance(new BigDecimal(initialCreditAmount))
                .totalCreditsEarned(new BigDecimal(initialCreditAmount))
                .totalCreditsUsed(BigDecimal.ZERO)
                .advancedBalance(new BigDecimal(initialAdvancedCreditAmount))
                .totalAdvancedCreditsEarned(new BigDecimal(initialAdvancedCreditAmount))
                .totalAdvancedCreditsUsed(BigDecimal.ZERO)
                .createdAt(now)
                .lastUpdated(now)
                .deleted(false)
                .build();
        
        return creditRepository.save(credit);
    }

    private Mono<Credit> updateCreditBalance(Credit credit, CreditTransaction transaction) {
        // Check if type is a String or enum and handle accordingly
        BigDecimal newBalance;
        if (transaction.getType2() != null) {
            // Use enum type2 field
            newBalance = transaction.getType2() == CreditTransaction.TransactionType.CREDIT
                    ? credit.getBalance().add(transaction.getAmount())
                    : credit.getBalance().subtract(transaction.getAmount()); // Fazla nokta kaldırıldı

            credit.setBalance(newBalance);
            credit.setLastUpdated(LocalDateTime.now());

            if (transaction.getType2() == CreditTransaction.TransactionType.CREDIT) {
                credit.setTotalCreditsEarned(credit.getTotalCreditsEarned().add(transaction.getAmount()));
            } else {
                credit.setTotalCreditsUsed(credit.getTotalCreditsUsed().add(transaction.getAmount()));
            }
        } else {
            // Use string type field
            newBalance = "CREDIT".equals(transaction.getType())
                    ? credit.getBalance().add(transaction.getAmount())
                    : credit.getBalance().subtract(transaction.getAmount());
                    
            credit.setBalance(newBalance);
            credit.setLastUpdated(LocalDateTime.now());

            if ("CREDIT".equals(transaction.getType())) {
                credit.setTotalCreditsEarned(credit.getTotalCreditsEarned().add(transaction.getAmount()));
            } else {
                credit.setTotalCreditsUsed(credit.getTotalCreditsUsed().add(transaction.getAmount()));
            }
        }

        return creditRepository.save(credit);
    }

    // Kredi olayını yayınlama ve metrik kayıt fonksiyonlarını güncelle
    private void publishCreditEvent(String userId, BigDecimal amount, String type, String creditType) {
        if (!kafkaEnabled) {
            log.debug("Kafka messaging is disabled, skipping event for user ID: {}", userId);
            return;
        }
        
        CreditEvent event = CreditEvent.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .creditType(creditType)
                .timestamp(System.currentTimeMillis())
                .build();
        
        try {
            kafkaTemplate.send("credit-events", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send credit event for user: {}, error: {}", 
                               userId, ex.getMessage());
                    } else {
                        log.debug("Credit event sent successfully for user: {}", userId);
                    }
                });
        } catch (Exception e) {
            log.error("Error attempting to send Kafka message for user: {}, error: {}", 
                     userId, e.getMessage());
            // Hata durumunda servisin çalışmasını engellememek için exception'ı yutuyoruz
        }
    }

    private void recordCreditMetrics(String userId, BigDecimal amount, String type, String creditType) {
        String metricName = type.equals("DEBIT") ? "credits.used" : "credits.added";
        if ("ADVANCED".equals(creditType)) {
            metricName = "advanced." + metricName;
        }
        
        meterRegistry.counter(metricName, "userId", userId)
                .increment(amount.doubleValue());
    }

    public Mono<Credit> getUserCredit(String userId) {
        return creditRepository.findByUserIdAndDeletedFalse(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating new credit record for user: {}", userId);
                    Credit newCredit = Credit.builder()
                            .userId(userId)
                            .balance(new BigDecimal("0.0"))
                            .totalCreditsEarned(new BigDecimal("0.0"))
                            .totalCreditsUsed(new BigDecimal("0.0"))
                            .advancedBalance(new BigDecimal("0.0"))
                            .totalAdvancedCreditsEarned(new BigDecimal("0.0"))
                            .totalAdvancedCreditsUsed(new BigDecimal("0.0"))
                            .lifetimeEarned(0.0)
                            .lifetimeSpent(0.0)
                            .deleted(false)
                            .createdAt(LocalDateTime.now())
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    return creditRepository.save(newCredit);
                }));
    }

    public Mono<CreditTransaction> addCredits(String userId, double amount, CreditType creditType, String description) {
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(amount))
                .type(CreditTransaction.TransactionType.CREDIT.toString())
                .type2(CreditTransaction.TransactionType.CREDIT)
                .status(CreditTransaction.TransactionStatus.PENDING)
                .creditType(creditType.toString())
                .creditTypeEnum(creditType)
                .description(description)
                .timestamp(LocalDateTime.now())
                .deleted(false)
                .build();
                
        return transactionRepository.save(transaction)
                .flatMap(savedTransaction -> processTransaction(savedTransaction));
    }

    public Mono<CreditTransaction> useCredits(String userId, double amount, String description) {
        return getUserCredit(userId)
                .filter(credit -> credit.getBalance().doubleValue() >= amount)
                .switchIfEmpty(Mono.error(new InsufficientCreditsException("Insufficient credits")))
                .flatMap(credit -> {
                    CreditTransaction transaction = CreditTransaction.builder()
                            .userId(userId)
                            .amount(BigDecimal.valueOf(amount))
                            .type(CreditTransaction.TransactionType.DEBIT.toString())
                            .type2(CreditTransaction.TransactionType.DEBIT)
                            .status(CreditTransaction.TransactionStatus.PENDING)
                            .description(description)
                            .timestamp(LocalDateTime.now())
                            .deleted(false)
                            .build();
                    return transactionRepository.save(transaction);
                })
                .flatMap(this::processTransaction);
    }
    
    private Mono<CreditTransaction> processTransaction(CreditTransaction transaction) {
        return creditRepository.findByUserIdAndDeletedFalse(transaction.getUserId())
                .flatMap(credit -> {
                    if (transaction.getType2() == CreditTransaction.TransactionType.CREDIT) {
                        credit.setBalance(credit.getBalance().add(transaction.getAmount()));
                        credit.setLifetimeEarned(credit.getLifetimeEarned() + transaction.getAmount().doubleValue());
                    } else {
                        credit.setBalance(credit.getBalance().subtract(transaction.getAmount()));
                        credit.setLifetimeSpent(credit.getLifetimeSpent() + transaction.getAmount().doubleValue());
                    }
                    credit.setLastUpdated(LocalDateTime.now());
                    return creditRepository.save(credit);
                })
                .then(Mono.fromCallable(() -> {
                    transaction.setStatus(CreditTransaction.TransactionStatus.COMPLETED);
                    transaction.setUpdatedAt(new Date());
                    return transaction;
                }))
                .flatMap(transactionRepository::save);
    }

    public Mono<Void> processPendingTransactions() {
        return transactionRepository.findByStatusAndDeletedFalse(CreditTransaction.TransactionStatus.PENDING)
                .flatMap(this::processTransaction)
                .then();
    }
}