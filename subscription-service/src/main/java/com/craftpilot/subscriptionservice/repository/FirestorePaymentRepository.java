package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import com.google.cloud.firestore.CollectionReference;
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
public class FirestorePaymentRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "payments";

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

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

    public Flux<Payment> findByStatus(PaymentStatus status) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("status", status)
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
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("status", PaymentStatus.PENDING.name())
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

    public Mono<Void> deleteById(String id) {
        return Mono.fromCallable(() -> {
            getCollection().document(id).delete().get();
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
} 