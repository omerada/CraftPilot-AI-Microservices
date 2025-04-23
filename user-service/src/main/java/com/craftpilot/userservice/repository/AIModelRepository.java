package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.ai.AIModel;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AIModelRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "aiModels";
    
    public Mono<AIModel> save(AIModel model) {
        return Mono.fromCallable(() -> {
            try {
                // Orijinal ID'yi modelId alanında sakla
                String originalModelId = model.getId();
                
                // Model ID'si varsa, bunu modelId alanına kopyala
                if (originalModelId != null && !originalModelId.isEmpty()) {
                    model.setModelId(originalModelId);
                }
                
                // Yeni benzersiz bir belge ID'si oluştur
                String documentId = UUID.randomUUID().toString();
                model.setId(documentId);
                
                log.info("AI Model kaydediliyor: documentId={}, modelId={}", documentId, model.getModelId());
                
                // Firestore'a yeni ID ile kaydet
                firestore.collection(COLLECTION_NAME)
                        .document(documentId)
                        .set(model)
                        .get();
                
                return model;
            } catch (InterruptedException | ExecutionException e) {
                log.error("AI model kaydedilirken hata oluştu: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving AI model", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    public Flux<AIModel> findAll() {
        return Flux.defer(() -> {
            try {
                return Flux.fromIterable(
                        firestore.collection(COLLECTION_NAME).get().get().getDocuments())
                        .map(doc -> doc.toObject(AIModel.class));
            } catch (InterruptedException | ExecutionException e) {
                return Flux.error(new RuntimeException("Error retrieving all AI models", e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    public Flux<AIModel> findByCategory(String category) {
        return Flux.defer(() -> {
            try {
                return Flux.fromIterable(
                        firestore.collection(COLLECTION_NAME)
                                .whereEqualTo("category", category)
                                .get().get().getDocuments())
                        .map(doc -> doc.toObject(AIModel.class));
            } catch (InterruptedException | ExecutionException e) {
                return Flux.error(new RuntimeException("Error retrieving AI models by category", e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    public Mono<AIModel> findById(String id) {
        return Mono.fromCallable(() -> {
            try {
                // Önce doğrudan belge ID'si ile arama yap
                DocumentSnapshot docSnapshot = firestore.collection(COLLECTION_NAME)
                        .document(id)
                        .get()
                        .get();
                
                if (docSnapshot.exists()) {
                    return docSnapshot.toObject(AIModel.class);
                }
                
                // Belge bulunamadıysa, modelId alanına göre sorgula
                return firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("modelId", id)
                        .get()
                        .get()
                        .getDocuments()
                        .stream()
                        .findFirst()
                        .map(doc -> doc.toObject(AIModel.class))
                        .orElse(null);
            } catch (InterruptedException | ExecutionException e) {
                log.error("AI model ID ile getirilirken hata: {}", e.getMessage(), e);
                throw new RuntimeException("Error retrieving AI model by ID", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(String id) {
        return Mono.fromRunnable(() -> {
            try {
                firestore.collection(COLLECTION_NAME).document(id).delete().get();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting AI model", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Flux<AIModel> findByRequiredPlan(String plan) {
        return Mono.fromCallable(() -> {
            try {
                return firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("requiredPlan", plan)
                        .get()
                        .get()
                        .getDocuments()
                        .stream()
                        .map(doc -> doc.toObject(AIModel.class))
                        .toList();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error retrieving AI models by plan", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    private <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        apiFuture.addListener(() -> {
            try {
                completableFuture.complete(apiFuture.get());
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        }, Runnable::run);
        return completableFuture;
    }
}
