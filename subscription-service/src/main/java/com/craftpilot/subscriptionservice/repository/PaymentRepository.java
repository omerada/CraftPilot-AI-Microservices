package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "payments";

    public Mono<Payment> save(Payment payment) {
        if (payment.getId() == null) {
            payment.setId(UUID.randomUUID().toString());
        }
        
        return Mono.fromCallable(() -> {
            firestore.collection(COLLECTION_NAME)
                    .document(payment.getId())
                    .set(payment)
                    .get();
            return payment;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Payment> findById(String id) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(Payment.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Payment> findByUserId(String userId) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Payment.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Payment> findBySubscriptionId(String subscriptionId) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("subscriptionId", subscriptionId)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Payment.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Payment> findByIyzicoPaymentId(String iyzicoPaymentId) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("iyzicoPaymentId", iyzicoPaymentId)
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(doc -> doc.toObject(Payment.class))
                    .orElse(null)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Payment> findByStatus(PaymentStatus status) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("status", status.name())
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Payment.class))
                    .toList()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Payment> findPendingPayments() {
        return findByStatus(PaymentStatus.PENDING);
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(id).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting payment", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
} 