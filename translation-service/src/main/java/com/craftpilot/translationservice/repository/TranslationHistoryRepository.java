package com.craftpilot.translationservice.repository;

import com.craftpilot.translationservice.model.TranslationHistory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TranslationHistoryRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "translation_history";

    public Mono<TranslationHistory> save(TranslationHistory translationHistory) {
        return Mono.create(sink -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document();
                translationHistory.setId(docRef.getId());
                ApiFuture<WriteResult> result = docRef.set(translationHistory);
                result.get();
                sink.success(translationHistory);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Mono<TranslationHistory> findById(String id) {
        return Mono.create(sink -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    sink.success(document.toObject(TranslationHistory.class));
                } else {
                    sink.success();
                }
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Mono<Void> delete(String id) {
        return Mono.create(sink -> {
            try {
                ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME).document(id).delete();
                future.get();
                sink.success();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Flux<TranslationHistory> findByUserId(String userId) {
        return Mono.<List<TranslationHistory>>create(sink -> {
            try {
                ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("userId", userId)
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .limit(5)
                        .get();
                
                List<TranslationHistory> histories = new ArrayList<>();
                future.get().getDocuments().forEach(doc -> 
                    histories.add(doc.toObject(TranslationHistory.class)));
                
                sink.success(histories);
            } catch (Exception e) {
                sink.error(e);
            }
        }).flatMapMany(Flux::fromIterable);
    }
} 