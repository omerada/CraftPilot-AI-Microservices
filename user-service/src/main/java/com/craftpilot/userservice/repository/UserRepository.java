package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private static final String COLLECTION_NAME = "users";
    private final Firestore firestore;

    public boolean existsByEmail(String email) {
        try {
            ApiFuture<QuerySnapshot> query = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get();
            QuerySnapshot querySnapshot = query.get();
            return !querySnapshot.isEmpty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error checking email existence", e);
        }
    }

    public Optional<UserEntity> findByEmail(String email) {
        try {
            var query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get();
            
            var documents = query.get().getDocuments();
            return documents.isEmpty() ? 
                   Optional.empty() : 
                   Optional.ofNullable(documents.get(0).toObject(UserEntity.class));
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", e.getMessage());
            throw new RuntimeException("Error fetching user by email", e);
        }
    }

    public Optional<UserEntity> findById(String uid) {
        try {
            var docRef = firestore.collection(COLLECTION_NAME).document(uid);
            var document = docRef.get().get();
            
            return document.exists() ? 
                   Optional.ofNullable(document.toObject(UserEntity.class)) : 
                   Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", e.getMessage());
            throw new RuntimeException("Error fetching user by ID", e);
        }
    }

    public void save(UserEntity user) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(user.getUid())
                    .set(user)
                    .get(); // Wait for operation to complete
        } catch (Exception e) {
            log.error("Error saving user: {}", e.getMessage());
            throw new RuntimeException("Error saving user", e);
        }
    }

    public List<UserEntity> findAll() {
        try {
            ApiFuture<QuerySnapshot> query = firestore.collection("users").get();
            QuerySnapshot querySnapshot = query.get();
            return querySnapshot.toObjects(UserEntity.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching all users", e);
        }
    }
}
