package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "chatHistories";

    public Flux<ChatHistory> findAllByUserId(String userId) {
        return Flux.create(emitter -> {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get();

            future.addListener(() -> {
                try {
                    List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                    for (DocumentSnapshot document : documents) {
                        ChatHistory history = document.toObject(ChatHistory.class);
                        emitter.next(history);
                    }
                    emitter.complete();
                } catch (Exception e) {
                    emitter.error(e);
                }
            }, Runnable::run);
        });
    }

    public Mono<ChatHistory> findById(String id) {
        return Mono.create(emitter -> {
            ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION_NAME).document(id).get();
            future.addListener(() -> {
                try {
                    DocumentSnapshot document = future.get();
                    if (document.exists()) {
                        emitter.success(document.toObject(ChatHistory.class));
                    } else {
                        emitter.success();
                    }
                } catch (Exception e) {
                    emitter.error(e);
                }
            }, Runnable::run);
        });
    }

    public Mono<ChatHistory> save(ChatHistory chatHistory) {
        return Mono.create(emitter -> {
            if (chatHistory.getId() == null) {
                // Yeni belge oluştur
                chatHistory.setCreatedAt(Timestamp.now());
                chatHistory.setUpdatedAt(Timestamp.now());
                
                ApiFuture<DocumentReference> future = firestore.collection(COLLECTION_NAME).add(chatHistory);
                future.addListener(() -> {
                    try {
                        DocumentReference ref = future.get();
                        chatHistory.setId(ref.getId());
                        emitter.success(chatHistory);
                    } catch (Exception e) {
                        emitter.error(e);
                    }
                }, Runnable::run);
            } else {
                // Mevcut belgeyi güncelle
                chatHistory.setUpdatedAt(Timestamp.now());
                
                ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME)
                        .document(chatHistory.getId())
                        .set(chatHistory);
                
                future.addListener(() -> {
                    try {
                        future.get();
                        emitter.success(chatHistory);
                    } catch (Exception e) {
                        emitter.error(e);
                    }
                }, Runnable::run);
            }
        });
    }

    public Mono<Void> delete(String id) {
        return Mono.create(emitter -> {
            ApiFuture<WriteResult> future = firestore.collection(COLLECTION_NAME).document(id).delete();
            future.addListener(() -> {
                try {
                    future.get();
                    emitter.success();
                } catch (Exception e) {
                    emitter.error(e);
                }
            }, Runnable::run);
        });
    }

    public Mono<ChatHistory> addConversation(String historyId, Conversation conversation) {
        return Mono.create(emitter -> {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(historyId);
            
            firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();
                ChatHistory history = snapshot.toObject(ChatHistory.class);
                
                if (history != null) {
                    if (history.getConversations() == null) {
                        history.setConversations(List.of(conversation));
                    } else {
                        history.getConversations().add(conversation);
                    }
                    
                    history.setUpdatedAt(Timestamp.now());
                    transaction.set(docRef, history);
                }
                
                return history;
            }).addListener(() -> {
                try {
                    ChatHistory updatedHistory = findById(historyId).block();
                    emitter.success(updatedHistory);
                } catch (Exception e) {
                    emitter.error(e);
                }
            }, Runnable::run);
        });
    }

    public Mono<ChatHistory> updateTitle(String historyId, String newTitle) {
        return Mono.create(emitter -> {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(historyId);
            
            ApiFuture<WriteResult> future = docRef.update(
                "title", newTitle,
                "updatedAt", Timestamp.now()
            );
            
            future.addListener(() -> {
                try {
                    future.get();
                    ChatHistory updatedHistory = findById(historyId).block();
                    emitter.success(updatedHistory);
                } catch (Exception e) {
                    emitter.error(e);
                }
            }, Runnable::run);
        });
    }
}
