package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MongoPaymentRepository extends ReactiveMongoRepository<Payment, String> {
    Flux<Payment> findByUserId(String userId);

    Flux<Payment> findBySubscriptionId(String subscriptionId);

    Flux<Payment> findByStatus(PaymentStatus status);

    Flux<Payment> findByStatusEquals(PaymentStatus status); // alternatif sorgu methodu

    default Flux<Payment> findPendingPayments() {
        return findByStatus(PaymentStatus.PENDING);
    }
}
