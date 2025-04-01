package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.repository.ChatHistoryRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import com.craftpilot.llmservice.model.response.CategoryData;
import com.craftpilot.llmservice.model.response.ChatItem;
import com.craftpilot.llmservice.model.response.PaginatedChatHistoryResponse;
import com.craftpilot.llmservice.model.response.PaginationInfo;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;

    public Flux<ChatHistory> getChatHistoriesByUserId(String userId, int page, int pageSize) {
        if (userId == null || userId.isEmpty()) {
            log.warn("Geçersiz userId ile sohbet geçmişi istendi: {}", userId);
            return Flux.empty();
        }
        
        // Sayfalama parametrelerini doğrula
        if (page < 1) {
            page = 1;
        }
        
        if (pageSize < 1 || pageSize > 100) { // Maksimum sayfa boyutunu sınırla
            pageSize = 10;
        }
        
        log.info("Kullanıcının sohbet geçmişleri getiriliyor: {}, sayfa: {}, sayfa boyutu: {}", 
                userId, page, pageSize);
        
        return chatHistoryRepository.findAllByUserId(userId, page, pageSize)
                .doOnError(error -> log.error("Sohbet geçmişi getirirken hata: {}", error.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    public Mono<ChatHistory> getChatHistoryById(String id) {
        if (id == null || id.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi istendi");
            return Mono.empty();
        }
        
        log.debug("Sohbet geçmişi getiriliyor, ID: {}", id);
        return chatHistoryRepository.findById(id)
                .doOnError(error -> log.error("ID ile sohbet geçmişi getirirken hata, ID {}: {}", id, error.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<ChatHistory> createChatHistory(ChatHistory chatHistory) {
        if (chatHistory == null) {
            log.warn("Null sohbet geçmişi oluşturma isteği");
            return Mono.error(new IllegalArgumentException("Sohbet geçmişi boş olamaz"));
        }
        
        // ID yoksa oluştur
        if (chatHistory.getId() == null) {
            chatHistory.setId(UUID.randomUUID().toString());
        }
        
        // Timestamp kontrolü
        if (chatHistory.getCreatedAt() == null) {
            chatHistory.setCreatedAt(Timestamp.now());
        }
        if (chatHistory.getUpdatedAt() == null) {
            chatHistory.setUpdatedAt(Timestamp.now());
        }
        
        // Conversations null ise initialize et
        if (chatHistory.getConversations() == null) {
            chatHistory.setConversations(new ArrayList<>());
        }
        
        log.debug("Sohbet geçmişi oluşturuluyor: {}", chatHistory.getId());
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi oluştururken hata: {}", error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi oluşturulamadı: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> updateChatHistory(ChatHistory chatHistory) {
        if (chatHistory == null || chatHistory.getId() == null) {
            log.warn("Geçersiz sohbet geçmişi güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir sohbet geçmişi ve ID gerekli"));
        }
        
        // UpdatedAt'i her zaman güncelle
        chatHistory.setUpdatedAt(Timestamp.now());
        
        log.debug("Sohbet geçmişi güncelleniyor, ID: {}", chatHistory.getId());
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi güncellenirken hata, ID {}: {}", chatHistory.getId(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi güncellenemedi: " + e.getMessage(), e));
    }

    public Mono<Void> deleteChatHistory(String id) {
        if (id == null || id.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi silme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
        
        log.debug("Sohbet geçmişi siliniyor, ID: {}", id);
        return chatHistoryRepository.delete(id)
                .doOnError(error -> log.error("Sohbet geçmişi silinirken hata, ID {}: {}", id, error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi silinemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> addConversation(String historyId, Conversation conversation) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz sohbet ID ile mesaj ekleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir sohbet ID'si gerekli"));
        }
        
        if (conversation == null) {
            log.warn("Null konuşma ekleme isteği, Chat ID: {}", historyId);
            return Mono.error(new IllegalArgumentException("Geçerli bir mesaj gerekli"));
        }
        
        // Conversation ID yoksa oluştur
        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID().toString());
        }
        
        // Timestamp değeri yoksa, şu anki zamanı ekle
        if (conversation.getTimestamp() == null) {
            conversation.setTimestamp(Timestamp.now());
        }
        
        log.debug("Sohbete mesaj ekleniyor, Chat ID: {}, OrderIndex: {}", historyId, conversation.getOrderIndex());
        return chatHistoryRepository.addConversation(historyId, conversation)
                .doOnSuccess(result -> log.info("Mesaj başarıyla eklendi, Chat ID: {}, OrderIndex: {}", 
                                          historyId, conversation.getOrderIndex()))
                .doOnError(error -> log.error("Mesaj eklenirken hata, Chat ID {}, OrderIndex {}: {}", 
                                        historyId, conversation.getOrderIndex(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Mesaj eklenemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> updateChatHistoryTitle(String historyId, String newTitle) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet başlığı güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
        
        if (newTitle == null) {
            log.warn("Null başlık ile sohbet başlığı güncelleme isteği, ID: {}", historyId);
            newTitle = "Yeni Sohbet"; // Varsayılan başlık
        }
        
        log.debug("Sohbet başlığı güncelleniyor, ID: {}, Yeni başlık: {}", historyId, newTitle);
        return chatHistoryRepository.updateTitle(historyId, newTitle)
                .doOnError(error -> log.error("Sohbet başlığı güncellenirken hata, ID {}: {}", historyId, error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet başlığı güncellenemedi: " + e.getMessage(), e));
    }

    public Mono<ChatHistory> archiveChatHistory(String historyId) {
        log.info("Sohbet arşivleniyor, ID: {}", historyId);
        
        return chatHistoryRepository.findById(historyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Chat history with ID " + historyId + " not found")))
                .flatMap(chatHistory -> {
                    log.debug("Sohbet geçmişi bulundu, şu anki enable değeri: {}", chatHistory.isEnable());
                    chatHistory.setEnable(false);
                    chatHistory.setUpdatedAt(Timestamp.now());
                    
                    return chatHistoryRepository.save(chatHistory)
                            .doOnSuccess(updatedChat -> 
                                log.info("Sohbet başarıyla arşivlendi, ID: {}", updatedChat.getId()));
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        log.error("Sohbet arşivlenemedi: {}", e.getMessage());
                        return Mono.error(e);
                    }
                    log.error("Sohbet arşivlenirken hata: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Sohbet arşivlenemedi: " + e.getMessage(), e));
                });
    }
    
    public Mono<PaginatedChatHistoryResponse> getChatHistoriesByUserIdCategorized(
            String userId, int page, int pageSize, List<String> categoryFilters, 
            String searchQuery, String sortBy, String sortOrder) {
        log.info("Kategorize edilmiş sohbet geçmişi alınıyor: {}", userId);
        
        // If no categories are specified, use all categories in correct order
        final List<String> finalCategoryFilters = categoryFilters == null || categoryFilters.isEmpty() 
                ? List.of("today", "yesterday", "lastWeek", "lastMonth", "older")
                : categoryFilters;
        
        return getChatHistoriesByUserId(userId, 1, Integer.MAX_VALUE) // Get all histories first
                .collectList()
                .flatMap(allHistories -> {
                    // Veritabanındaki toplam kayıt sayısı (filtrelemeden önce)
                    int totalDatabaseRecords = allHistories.size();
                    
                    // Apply search filter if specified
                    List<ChatHistory> filteredHistories = allHistories;
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String query = searchQuery.toLowerCase();
                        filteredHistories = allHistories.stream()
                                .filter(history -> history.getTitle() != null && 
                                        history.getTitle().toLowerCase().contains(query))
                                .collect(Collectors.toList());
                    }
                    
                    // Sort the histories
                    Comparator<ChatHistory> comparator;
                    if ("createdAt".equals(sortBy)) {
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getCreatedAt()));
                    } else {
                        // Default to updatedAt
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getUpdatedAt()));
                    }
                    
                    // Apply sort order
                    if ("asc".equals(sortOrder)) {
                        // Keep original comparator for ascending
                    } else {
                        // Default to descending
                        comparator = comparator.reversed();
                    }
                    
                    filteredHistories = filteredHistories.stream()
                            .sorted(comparator)
                            .collect(Collectors.toList());
                    
                    // Group histories by category (today, yesterday, etc.)
                    Map<String, List<ChatHistory>> categorizedHistories = categorizeHistories(filteredHistories);
                    
                    // Create the response structure with LinkedHashMap to maintain order
                    LinkedHashMap<String, CategoryData> categories = new LinkedHashMap<>();
                    
                    // Bu sayfada gösterilecek toplam öğe sayısı
                    int displayedItems = 0;
                    int totalCategorizedItems = 0;
                    
                    // Önce tüm kategorilerin toplam öğe sayısını hesapla
                    for (String category : finalCategoryFilters) {
                        List<ChatHistory> histories = categorizedHistories.getOrDefault(category, List.of());
                        totalCategorizedItems += histories.size();
                    }
                    
                    // Sayfalama için skip ve limit değerlerini hesapla
                    int skipCount = (page - 1) * pageSize;
                    int remainingItems = Math.min(pageSize, totalCategorizedItems - skipCount);
                    
                    // Kategorileri işle ve gösterilecek öğeleri belirle
                    if (remainingItems > 0) {
                        for (String category : finalCategoryFilters) {
                            List<ChatHistory> histories = categorizedHistories.getOrDefault(category, List.of());
                            int categorySize = histories.size();
                            
                            if (skipCount >= categorySize) {
                                // Bu kategorinin tüm öğelerini atla
                                skipCount -= categorySize;
                                categories.put(category, new CategoryData(List.of(), categorySize));
                            } else {
                                // Bu kategoriden bazı öğeleri al
                                int itemsToTake = Math.min(remainingItems, categorySize - skipCount);
                                
                                if (itemsToTake > 0) {
                                    List<ChatItem> items = histories.stream()
                                            .skip(skipCount)
                                            .limit(itemsToTake)
                                            .map(this::convertToChatItem)
                                            .collect(Collectors.toList());
                                    
                                    categories.put(category, new CategoryData(items, categorySize));
                                    
                                    displayedItems += itemsToTake;
                                    remainingItems -= itemsToTake;
                                    skipCount = 0;
                                } else {
                                    categories.put(category, new CategoryData(List.of(), categorySize));
                                }
                            }
                            
                            if (remainingItems <= 0) {
                                break;
                            }
                        }
                    }
                    
                    // Kalan kategorileri boş listelerle doldur
                    for (String category : finalCategoryFilters) {
                        if (!categories.containsKey(category)) {
                            categories.put(category, new CategoryData(List.of(), 
                                categorizedHistories.getOrDefault(category, List.of()).size()));
                        }
                    }
                    
                    // Sayfalama bilgilerini hesapla (toplam kategori öğeleri üzerinden)
                    int totalPages = (int) Math.ceil((double) totalCategorizedItems / pageSize);
                    
                    // hasMore değerini veritabanındaki toplam kayıt sayısına göre hesapla
                    // Mevcut sayfa * sayfa boyutu, toplam kayıt sayısından küçükse, daha fazla kayıt vardır
                    boolean hasMore = page * pageSize < totalDatabaseRecords;
                    
                    PaginationInfo paginationInfo = PaginationInfo.builder()
                            .currentPage(page)
                            .totalPages(totalPages)
                            .pageSize(pageSize)
                            .totalItems(totalDatabaseRecords) // Toplam kayıt sayısını göster
                            .hasMore(hasMore)
                            .build();
                    
                    // Construct the final response
                    PaginatedChatHistoryResponse response = PaginatedChatHistoryResponse.builder()
                            .categories(categories)
                            .pagination(paginationInfo)
                            .build();
                    
                    return Mono.just(response);
                });
    }

    private long getTimestampValue(Timestamp timestamp) {
        return timestamp != null ? timestamp.getSeconds() : 0L;
    }

    private Map<String, List<ChatHistory>> categorizeHistories(List<ChatHistory> histories) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastWeekStart = today.minusDays(7);
        LocalDate lastMonthStart = today.minusDays(30);
        
        // Use LinkedHashMap to maintain insertion order
        Map<String, List<ChatHistory>> categorized = new LinkedHashMap<>();
        categorized.put("today", new ArrayList<>());
        categorized.put("yesterday", new ArrayList<>());
        categorized.put("lastWeek", new ArrayList<>());
        categorized.put("lastMonth", new ArrayList<>());
        categorized.put("older", new ArrayList<>());
        
        for (ChatHistory history : histories) {
            // Use updatedAt if available, otherwise use createdAt
            Timestamp timestamp = history.getUpdatedAt() != null ? history.getUpdatedAt() : history.getCreatedAt();
            LocalDate historyDate = getLocalDateFromTimestamp(timestamp);
            
            if (historyDate.equals(today)) {
                categorized.get("today").add(history);
            } else if (historyDate.equals(yesterday)) {
                categorized.get("yesterday").add(history);
            } else if (historyDate.isAfter(lastWeekStart)) {
                categorized.get("lastWeek").add(history);
            } else if (historyDate.isAfter(lastMonthStart)) {
                categorized.get("lastMonth").add(history);
            } else {
                categorized.get("older").add(history);
            }
        }
        
        return categorized;
    }

    private LocalDate getLocalDateFromTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return LocalDate.now(); // Default to today if no timestamp
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneId.systemDefault()
        ).toLocalDate();
    }

    private ChatItem convertToChatItem(ChatHistory history) {
        String lastConversation = null;
        if (history.getConversations() != null && !history.getConversations().isEmpty()) {
            // Find the last conversation based on orderIndex or timestamp
            Optional<Conversation> lastConv = history.getConversations().stream()
                    .max(Comparator.comparing(Conversation::getOrderIndex));
            
            if (lastConv.isPresent()) {
                lastConversation = lastConv.get().getContent();
                // Truncate if too long
                if (lastConversation != null && lastConversation.length() > 100) {
                    lastConversation = lastConversation.substring(0, 97) + "...";
                }
            }
        }
        
        return ChatItem.builder()
                .id(history.getId())
                .title(history.getTitle())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .lastConversation(lastConversation)
                .build();
    }
}
