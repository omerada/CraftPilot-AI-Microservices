package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "users";

    public Mono<UserEntity> save(UserEntity user) {
        return Mono.fromCallable(() -> {
            if (user.getId() == null) {
                user.setId(firestore.collection(COLLECTION_NAME).document().getId());
            }
            firestore.collection(COLLECTION_NAME).document(user.getId()).set(user).get();
            return user;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserEntity> findById(String id) {
        return Mono.fromCallable(() -> 
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(UserEntity.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(id).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting user", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<UserEntity> findByEmail(String email) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("email", email)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(doc -> doc.toObject(UserEntity.class))
                    .orElse(null)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserEntity> findByUsername(String username) {
        return Mono.fromCallable(() ->
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("username", username)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(doc -> doc.toObject(UserEntity.class))
                    .orElse(null)
        ).subscribeOn(Schedulers.boundedElastic());
    }
}
