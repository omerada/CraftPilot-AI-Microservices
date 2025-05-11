package com.craftpilot.creditservice.repository;

import com.craftpilot.creditservice.model.CreditTransaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.Date;

@Repository
public interface CreditTransactionRepository extends ReactiveMongoRepository<CreditTransaction, String> {
    Flux<CreditTransaction> findByUserIdAndDeletedFalse(String userId);
    Flux<CreditTransaction> findByStatusAndDeletedFalse(CreditTransaction.TransactionStatus status);
    Flux<CreditTransaction> findByTimestampAfterAndUserIdAndDeletedFalse(Date timestamp, String userId);
}