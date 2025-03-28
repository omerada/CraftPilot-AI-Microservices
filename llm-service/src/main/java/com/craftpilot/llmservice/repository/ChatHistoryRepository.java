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
                    // EKLEME ÖNCESİ KESİN NULL SEQUENCE KONTROLÜ
                    if (conversation.getSequence() == null || conversation.getSequence() == 0) {
                        // Eğer sequence değeri hala null veya 0 ise, ciddi bir hata var demektir
                        log.warn("Null sequence değeri ile ekleme yapılmaya çalışılıyor, otomatik düzeltiliyor");
                        
                        // Şu anki zamanı kullanarak sequence oluştur
                        long currentTime = System.currentTimeMillis();
                        
                        // Role'e göre sequence değeri ata
                        if ("user".equals(conversation.getRole())) {
                            conversation.setSequence(currentTime);
                        } else {
                            // AI mesajları için daha büyük bir değer
                            conversation.setSequence(currentTime + 1000);
                        }
                        
                        // Timestamp değerini de güncelle
                        conversation.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                            conversation.getSequence() / 1000,
                            (int) ((conversation.getSequence() % 1000) * 1_000_000)
                        ));
                        
                        log.info("Repository'de sequence değeri atandı: {}", conversation.getSequence());
                    }
                    
                    // Mevcut koleksiyonu kontrol et
                    if (history.getConversations() == null) {
                        history.setConversations(List.of(conversation));
                        log.debug("İlk mesaj ekleniyor: {}", conversation.getId());
                    } else {
                        // Mevcut mesajlarda null sequence kontrolü yap
                        List<Conversation> updatedConversations = new ArrayList<>();
                        long currentTime = System.currentTimeMillis();
                        
                        for (Conversation existingConv : history.getConversations()) {
                            // Eğer mevcut bir mesajda null sequence varsa düzelt
                            if (existingConv.getSequence() == null || existingConv.getSequence() == 0) {
                                log.warn("Mevcut mesajda null sequence bulundu, düzeltiliyor. ID: {}", existingConv.getId());
                                
                                // Timestamp'ten sequence türet
                                if (existingConv.getTimestamp() != null) {
                                    long seconds = existingConv.getTimestamp().getSeconds();
                                    int nanos = existingConv.getTimestamp().getNanos();
                                    existingConv.setSequence(seconds * 1000 + (nanos / 1_000_000));
                                } else {
                                    // Hem timestamp hem de sequence yoksa yeni bir değer ata
                                    existingConv.setSequence(currentTime - 10000); // Eski bir mesaj olduğunu varsay
                                    existingConv.setTimestamp(Timestamp.ofTimeSecondsAndNanos(
                                        existingConv.getSequence() / 1000,
                                        (int) ((existingConv.getSequence() % 1000) * 1_000_000)
                                    ));
                                }
                                
                                log.info("Mevcut mesaja sequence atandı: {}, ID: {}", 
                                        existingConv.getSequence(), existingConv.getId());
                            }
                            
                            updatedConversations.add(existingConv);
                        }
                        
                        // Yeni mesajı ekle
                        updatedConversations.add(conversation);
                        
                        // Tüm mesajları sequence'e göre sırala
                        history.setConversations(
                            updatedConversations.stream()
                                .sorted((a, b) -> {
                                    // Sequence bazlı karşılaştırma
                                    Long seqA = a.getSequence() != null ? a.getSequence() : 0L;
                                    Long seqB = b.getSequence() != null ? b.getSequence() : 0L;
                                    
                                    // Aynı sequence değeri olması durumunda role'e göre sırala
                                    if (Math.abs(seqA - seqB) < 100) {
                                        if ("user".equals(a.getRole()) && !"user".equals(b.getRole())) {
                                            return -1; // Kullanıcı mesajları önce
                                        } else if (!"user".equals(a.getRole()) && "user".equals(b.getRole())) {
                                            return 1; // AI mesajları sonra
                                        }
                                    }
                                    
                                    return seqA.compareTo(seqB);
                                })
                                .collect(Collectors.toList())
                        );
                        
                        log.debug("Mesaj eklendi ve sıralama yapıldı, toplam mesaj sayısı: {}", 
                                 history.getConversations().size());
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
