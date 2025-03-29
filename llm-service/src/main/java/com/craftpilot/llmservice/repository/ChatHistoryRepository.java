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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

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
            
            // Execute in a transaction for atomicity
            firestore.runTransaction(transaction -> {
                try {
                    DocumentSnapshot snapshot = transaction.get(docRef).get();
                    if (!snapshot.exists()) {
                        throw new IllegalArgumentException("Chat history with ID " + historyId + " does not exist");
                    }
                    
                    ChatHistory history = snapshot.toObject(ChatHistory.class);
                    if (history == null) {
                        throw new IllegalArgumentException("Failed to deserialize chat history");
                    }
                    
                    // Ensure conversation has an ID
                    if (conversation.getId() == null || conversation.getId().isEmpty()) {
                        conversation.setId(UUID.randomUUID().toString());
                        log.debug("Generated new conversation ID: {}", conversation.getId());
                    }
                    
                    // If conversations list is null, initialize it
                    if (history.getConversations() == null) {
                        history.setConversations(new ArrayList<>());
                    }
                    
                    // Yeni yaklaşım: Frontend'den gelen orderIndex değerine koşulsuz güven
                    // Hiçbir koşulda değiştirme, sadece gerektiğinde tamamla
                    if (conversation.getOrderIndex() == null) {
                        // Sadece null ise en yüksek değeri hesapla ve 1 ekle
                        int highestIndex = 0;
                        for (Conversation existingConv : history.getConversations()) {
                            if (existingConv.getOrderIndex() != null && existingConv.getOrderIndex() > highestIndex) {
                                highestIndex = existingConv.getOrderIndex();
                            }
                        }
                        conversation.setOrderIndex(highestIndex + 1);
                        log.info("OrderIndex was null, assigned: {} for conversation {}", 
                                 conversation.getOrderIndex(), conversation.getId());
                    } else {
                        // Frontend'den gelen değeri kullan, çakışma kontrolü yapma
                        log.info("Using frontend-provided orderIndex: {} for conversation {}", 
                                 conversation.getOrderIndex(), conversation.getId());
                    }
                    
                    log.info("Using orderIndex: {} for conversation {}, role {}", 
                            conversation.getOrderIndex(), conversation.getId(), conversation.getRole());
                    
                    // Ensure timestamp is set
                    if (conversation.getTimestamp() == null) {
                        conversation.setTimestamp(Timestamp.now());
                    }
                    
                    // Add the new conversation to the list
                    List<Conversation> updatedConversations = new ArrayList<>(history.getConversations());
                    updatedConversations.add(conversation);
                    
                    // Sıralama mantığını basitleştir ve sadece orderIndex'e göre sırala
                    updatedConversations.sort(
                        Comparator.comparing(
                            (Conversation c) -> c.getOrderIndex() != null ? c.getOrderIndex() : Integer.MAX_VALUE
                        )
                    );
                    
                    // Update the history object
                    history.setConversations(updatedConversations);
                    history.setUpdatedAt(Timestamp.now());
                    
                    // Update lastConversation field if it exists in the model
                    if (conversation.getContent() != null) {
                        history.setLastConversation(conversation.getContent());
                    }
                    
                    // Update the document in Firestore
                    transaction.set(docRef, history);
                    return history;
                } catch (Exception e) {
                    log.error("Error during transaction: {}", e.getMessage(), e);
                    throw e;
                }
            }).addListener(() -> {
                try {
                    // Get the latest version of the history after the transaction
                    ChatHistory updatedHistory = findById(historyId).block();
                    
                    // Yanıtta atanan orderIndex'i loglayalım (debug için)
                    if (conversation != null) {
                        log.info("Added conversation: role={}, orderIndex={}, content={}", 
                            conversation.getRole(), conversation.getOrderIndex(), 
                            conversation.getContent() != null ? conversation.getContent().substring(0, Math.min(20, conversation.getContent().length())) + "..." : "null");
                    }
                    
                    emitter.success(updatedHistory);
                    log.info("Successfully added conversation {} to chat {}, total conversations: {}", 
                            conversation.getId(), historyId, 
                            updatedHistory != null && updatedHistory.getConversations() != null ? 
                                updatedHistory.getConversations().size() : 0);
                    
                    // Detaylı loglama ekleyelim
                    if (updatedHistory != null && updatedHistory.getConversations() != null) {
                        StringBuilder orderLog = new StringBuilder("Current orderIndex sequence: ");
                        for (Conversation c : updatedHistory.getConversations()) {
                            orderLog.append(c.getOrderIndex()).append("(").append(c.getRole()).append("), ");
                        }
                        log.info(orderLog.toString());
                    }
                } catch (Exception e) {
                    log.error("Error retrieving updated chat history after adding conversation: {}", e.getMessage(), e);
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
