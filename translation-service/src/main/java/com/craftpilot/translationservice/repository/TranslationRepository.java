package com.craftpilot.translationservice.repository;

import com.craftpilot.translationservice.model.Translation;
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
public class TranslationRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "translations";

    public Mono<Translation> save(Translation translation) {
        return Mono.create(sink -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document();
                translation.setId(docRef.getId());
                ApiFuture<WriteResult> result = docRef.set(translation);
                result.get();
                sink.success(translation);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Mono<Translation> findById(String id) {
        return Mono.create(sink -> {
            try {
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    sink.success(document.toObject(Translation.class));
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

    public Flux<Translation> findByUserId(String userId) {
        return Mono.<List<Translation>>create(sink -> {
            try {
                ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("userId", userId)
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .limit(5)
                        .get();
                
                List<Translation> translations = new ArrayList<>();
                future.get().getDocuments().forEach(doc -> 
                    translations.add(doc.toObject(Translation.class)));
                
                sink.success(translations);
            } catch (Exception e) {
                sink.error(e);
            }
        }).flatMapMany(Flux::fromIterable);
    }
} 