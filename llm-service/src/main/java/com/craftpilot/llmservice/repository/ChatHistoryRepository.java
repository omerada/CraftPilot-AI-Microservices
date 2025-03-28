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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
                
                // HANDLING TIMESTAMPS AND SEQUENCES - SIMPLIFIED APPROACH
                long currentTimeMillis = System.currentTimeMillis();
                
                // If conversations list is null, initialize it
                if (history.getConversations() == null) {
                    history.setConversations(new ArrayList<>());
                }
                
                // Get the highest existing sequence value
                long highestSequence = 0;
                for (Conversation existingConv : history.getConversations()) {
                    if (existingConv.getSequence() != null && existingConv.getSequence() > highestSequence) {
                        highestSequence = existingConv.getSequence();
                    }
                }
                
                // SIMPLE SEQUENTIAL APPROACH: Always increment from the highest existing sequence
                // Use at least the current time in milliseconds for completely new chats
                long newSequence = Math.max(highestSequence + 1000, currentTimeMillis);
                
                // Set the new sequence value
                conversation.setSequence(newSequence);
                log.info("Assigned sequential value: {} for conversation {}, role {}", 
                        newSequence, conversation.getId(), conversation.getRole());
                
                // Set or update the timestamp to match the sequence
                conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                    newSequence / 1000,
                    (int) ((newSequence % 1000) * 1_000_000)
                ));
                
                // Add the new conversation to the list
                List<Conversation> updatedConversations = new ArrayList<>(history.getConversations());
                updatedConversations.add(conversation);
                
                // Sort by sequence (simplest approach)
                updatedConversations.sort(Comparator.comparing(
                    c -> c.getSequence() != null ? c.getSequence() : 0L
                ));
                
                // Update the history object
                history.setConversations(updatedConversations);
                history.setUpdatedAt(Timestamp.now());
                
                // Son konuşma içeriğini lastConversation alanına ekle
                if (conversation.getContent() != null && !conversation.getContent().isEmpty()) {
                    // Doğrudan field'a erişim yerine lastConversation değerini atayalımdirekt kullanacağız
                    history.setLastConversation(conversation.getContent());    history.lastConversation = conversation.getContent();
                }
                
                // Update the document in Firestoreocument in Firestore
                transaction.set(docRef, history);cRef, history);
                return history;n history;
            }).addListener(() -> {
                try {
                    // Get the latest version of the history after the transaction history after the transaction
                    ChatHistory updatedHistory = findById(historyId).block();
                    emitter.success(updatedHistory);
                    log.info("Successfully added conversation {} to chat {}, total conversations: {}", {}", 
                            conversation.getId(), historyId, 
                            updatedHistory != null && updatedHistory.getConversations() != null ? ory != null && updatedHistory.getConversations() != null ? 
                                updatedHistory.getConversations().size() : 0);
                } catch (Exception e) { {
                    log.error("Error retrieving updated chat history after adding conversation: {}", e.getMessage(), e);   log.error("Error retrieving updated chat history after adding conversation: {}", e.getMessage(), e);
                    emitter.error(e);ror(e);
                }     }
            }, Runnable::run);       }, Runnable::run);
        });        });
    }

    public Mono<ChatHistory> updateTitle(String historyId, String newTitle) {
        return Mono.create(emitter -> {rn Mono.create(emitter -> {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(historyId);n(COLLECTION_NAME).document(historyId);
            
            ApiFuture<WriteResult> future = docRef.update(docRef.update(
                "title", newTitle,  "title", newTitle,
                "updatedAt", Timestamp.now()    "updatedAt", Timestamp.now()
            );
            
            future.addListener(() -> { -> {
                try {
                    future.get();
                    ChatHistory updatedHistory = findById(historyId).block();History = findById(historyId).block();
                    emitter.success(updatedHistory);pdatedHistory);
                } catch (Exception e) { catch (Exception e) {
                    emitter.error(e);ror(e);
                }     }
            }, Runnable::run);       }, Runnable::run);
        });       });
    }    }


}}
