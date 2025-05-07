package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.exception.FirebaseAuthException;
import com.craftpilot.usermemoryservice.model.UserInstruction;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class UserInstructionRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "user_instructions";

    public UserInstructionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Flux<UserInstruction> findByUserId(String userId) {
        log.info("Fetching user instructions for userId: {}", userId);
        
        return Mono.fromCallable(() -> {
            try {
                QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();
                
                return querySnapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(UserInstruction.class))
                    .toList();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının gerekli izinleri yok", e);
                }
                
                log.error("Error fetching user instructions: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching user instructions", e);
            } catch (Exception e) {
                log.error("Unexpected error fetching user instructions: {}", e.getMessage(), e);
                throw new RuntimeException("Error accessing user instructions data", e);
            }
        })
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserInstruction> findById(String instructionId) {
        log.info("Fetching user instruction by id: {}", instructionId);
        
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(instructionId);
                DocumentSnapshot snapshot = docRef.get().get();
                
                if (snapshot.exists()) {
                    return snapshot.toObject(UserInstruction.class);
                } else {
                    log.info("No instruction found with id: {}", instructionId);
                    return null;
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının gerekli izinleri yok", e);
                }
                
                log.error("Error fetching instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching instruction", e);
            } catch (Exception e) {
                log.error("Unexpected error fetching instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error accessing instruction data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserInstruction> save(UserInstruction instruction) {
        if (instruction.getId() == null) {
            instruction.setId(UUID.randomUUID().toString());
        }
        
        return Mono.fromCallable(() -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(instruction.getId());
                docRef.set(instruction).get();
                log.info("User instruction saved successfully with id: {}", instruction.getId());
                return instruction;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied during save: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının yazma izni yok", e);
                }
                
                log.error("Error saving user instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving user instruction", e);
            } catch (Exception e) {
                log.error("Unexpected error saving user instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving user instruction data", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String instructionId) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(instructionId).delete().get();
                log.info("User instruction deleted successfully with id: {}", instructionId);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PermissionDeniedException) {
                    log.error("Firebase permission denied during delete: {}", e.getCause().getMessage());
                    throw new FirebaseAuthException(
                            "Firebase yetkilendirme hatası: Servis hesabının silme izni yok", e);
                }
                
                log.error("Error deleting user instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error deleting user instruction", e);
            } catch (Exception e) {
                log.error("Unexpected error deleting user instruction: {}", e.getMessage(), e);
                throw new RuntimeException("Error deleting user instruction", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> deleteByUserId(String userId) {
        return findByUserId(userId)
            .map(UserInstruction::getId)
            .flatMap(this::deleteById)
            .then();
    }
}
