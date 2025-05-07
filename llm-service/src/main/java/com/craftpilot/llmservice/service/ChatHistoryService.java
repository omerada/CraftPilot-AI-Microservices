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
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import com.craftpilot.llmservice.model.response.CategoryData;
import com.craftpilot.llmservice.model.response.ChatItem;
import com.craftpilot.llmservice.model.response.PaginatedChatHistoryResponse;
import com.craftpilot.llmservice.model.response.PaginationInfo;

import com.craftpilot.commons.activity.annotation.LogActivity;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import com.craftpilot.commons.activity.model.ActivityEventTypes;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;
    private final ActivityLogger activityLogger;  

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
         
        return chatHistoryRepository.findAllByUserId(userId, page, pageSize)
                .doOnError(error -> log.error("Sohbet geçmişi getirirken hata: {}", error.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    public Mono<ChatHistory> getChatHistoryById(String id) {
        if (id == null || id.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi istendi");
            return Mono.empty();
        }
         
        return chatHistoryRepository.findById(id)
                .doOnError(error -> log.error("ID ile sohbet geçmişi getirirken hata, ID {}: {}", id, error.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    @LogActivity(
        actionType = ActivityEventTypes.CHAT_HISTORY_CREATE, 
        userIdParam = "#chatHistory.userId",
        metadata = "{\"id\": #result.id, \"title\": #result.title}"
    )
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
         
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi oluştururken hata: {}", error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi oluşturulamadı: " + e.getMessage(), e));
    }

    @LogActivity(
        actionType = ActivityEventTypes.CHAT_HISTORY_UPDATE, 
        userIdParam = "#chatHistory.userId",
        metadata = "{\"id\": #result.id, \"title\": #result.title, \"messageCount\": #result.conversations.size()}"
    )
    public Mono<ChatHistory> updateChatHistory(ChatHistory chatHistory) {
        if (chatHistory == null || chatHistory.getId() == null) {
            log.warn("Geçersiz sohbet geçmişi güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir sohbet geçmişi ve ID gerekli"));
        }
        
        // UpdatedAt'i her zaman güncelle
        chatHistory.setUpdatedAt(Timestamp.now());
         
        return chatHistoryRepository.save(chatHistory)
                .doOnError(error -> log.error("Sohbet geçmişi güncellenirken hata, ID {}: {}", chatHistory.getId(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet geçmişi güncellenemedi: " + e.getMessage(), e));
    }

    @LogActivity(
        actionType = ActivityEventTypes.CHAT_HISTORY_DELETE, 
        userIdParam = "#userId",
        metadata = "{\"id\": #historyId}"
    )
    public Mono<Void> deleteChatHistory(String userId, String historyId) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet geçmişi silme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
         
        return chatHistoryRepository.findById(historyId)
                .flatMap(history -> {
                    // Önce silme işlemini yap
                    return chatHistoryRepository.delete(historyId)  
                            .then(activityLogger.log(
                                userId,
                                ActivityEventTypes.CHAT_HISTORY_DELETE,
                                Map.of("id", historyId, "title", history.getTitle())
                            ))
                            .then();
                });
    }

    @LogActivity(
        actionType = ActivityEventTypes.CONVERSATION_CREATE,
        userIdParam = "#result.userId",
        metadata = "{historyId: #result.id, conversationId: #result.conversations[-1].id}"
    )
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
         
        return chatHistoryRepository.addConversation(historyId, conversation) 
                .doOnError(error -> log.error("Mesaj eklenirken hata, Chat ID {}, OrderIndex {}: {}", 
                                        historyId, conversation.getOrderIndex(), error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Mesaj eklenemedi: " + e.getMessage(), e));
    }

    @LogActivity(
        actionType = ActivityEventTypes.TITLE_UPDATE,
        userIdParam = "#result.userId",
        metadata = "{historyId: #result.id, newTitle: #result.title}"
    )
    public Mono<ChatHistory> updateChatHistoryTitle(String historyId, String newTitle) {
        if (historyId == null || historyId.isEmpty()) {
            log.warn("Geçersiz ID ile sohbet başlığı güncelleme isteği");
            return Mono.error(new IllegalArgumentException("Geçerli bir ID gerekli"));
        }
        
        if (newTitle == null) {
            log.warn("Null başlık ile sohbet başlığı güncelleme isteği, ID: {}", historyId);
            newTitle = "Yeni Sohbet"; // Varsayılan başlık
        }
         
        return chatHistoryRepository.updateTitle(historyId, newTitle)
                .doOnError(error -> log.error("Sohbet başlığı güncellenirken hata, ID {}: {}", historyId, error.getMessage()))
                .onErrorMap(e -> new RuntimeException("Sohbet başlığı güncellenemedi: " + e.getMessage(), e));
    }

    @LogActivity(
        actionType = ActivityEventTypes.CHAT_HISTORY_ARCHIVE, 
        userIdParam = "#userId",
        metadata = "{\"id\": #historyId}"
    )
    public Mono<ChatHistory> archiveChatHistory(String userId, String historyId) { 
        
        return chatHistoryRepository.findById(historyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Chat history with ID " + historyId + " not found")))
                .flatMap(chatHistory -> { 
                    chatHistory.setEnable(false);
                    chatHistory.setUpdatedAt(Timestamp.now());
                    
                    return chatHistoryRepository.save(chatHistory); 
                })  // Eksik parantez eklendi
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

    @LogActivity(
        actionType = ActivityEventTypes.CHAT_HISTORY_UNARCHIVE, 
        userIdParam = "#userId",
        metadata = "{\"id\": #historyId}"
    )
    public Mono<ChatHistory> unarchiveChatHistory(String userId, String historyId) { 
        
        return chatHistoryRepository.findById(historyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Chat history with ID " + historyId + " not found")))
                .flatMap(chatHistory -> { 
                    chatHistory.setEnable(true);
                    chatHistory.setUpdatedAt(Timestamp.now());
                    
                    return chatHistoryRepository.save(chatHistory); 
                })  // Eksik parantez eklendi
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        log.error("Sohbet arşivden çıkarılamadı: {}", e.getMessage());
                        return Mono.error(e);
                    }
                    log.error("Sohbet arşivden çıkarılırken hata: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Sohbet arşivden çıkarılamadı: " + e.getMessage(), e));
                });
    }

    /**
     * Belirli kullanıcının arşivlediği sohbet geçmişlerini sayfalanmış olarak getirir
     */
    public Mono<PaginatedChatHistoryResponse> getArchivedChatHistories(
            String userId, int page, int pageSize, String searchQuery, String sortBy, String sortOrder) { 
        
        // Sayfalama mantığını doğru hesaplamak için önce tüm kayıtları getirelim
        return chatHistoryRepository.findAllByUserId(userId, 1, Integer.MAX_VALUE)
                .collectList()
                .flatMap(allHistories -> {
                    // Sadece arşivlenmiş (enable=false) kayıtları filtreleme
                    List<ChatHistory> archivedHistories = allHistories.stream()
                            .filter(history -> !history.isEnable()) // Arşivlenmiş olanlar (enable=false)
                            .collect(Collectors.toList());
                    
                    // Arama filtresi uygula
                    List<ChatHistory> filteredHistories = archivedHistories;
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String query = searchQuery.toLowerCase();
                        filteredHistories = archivedHistories.stream()
                                .filter(history -> history.getTitle() != null && 
                                        history.getTitle().toLowerCase().contains(query))
                                .collect(Collectors.toList());
                    }
                    
                    // Sıralama uygula
                    Comparator<ChatHistory> comparator;
                    if ("createdAt".equals(sortBy)) {
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getCreatedAt()));
                    } else {
                        // Default to updatedAt
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getUpdatedAt()));
                    }
                    
                    // Sıralama yönü
                    if ("asc".equals(sortOrder)) {
                        // Ascending sıralama
                    } else {
                        // Default to descending
                        comparator = comparator.reversed();
                    }
                    
                    filteredHistories = filteredHistories.stream()
                            .sorted(comparator)
                            .collect(Collectors.toList());
                    
                    // Toplam kayıt sayısı
                    int totalFilteredRecords = filteredHistories.size();
                    
                    // Sayfalama için hesaplamalar
                    int skipCount = (page - 1) * pageSize;
                    int remainingItems = Math.min(pageSize, totalFilteredRecords - skipCount);
                    
                    // Sayfa için öğeleri al
                    List<ChatHistory> pagedHistories = filteredHistories.stream()
                            .skip(skipCount)
                            .limit(pageSize)
                            .collect(Collectors.toList());
                    
                    // ChatItem'lara dönüştür
                    List<ChatItem> items = pagedHistories.stream()
                            .map(this::convertToChatItem)
                            .collect(Collectors.toList());
                    
                    // Kategoriler için tek bir kategori oluştur: "archived"
                    LinkedHashMap<String, CategoryData> categories = new LinkedHashMap<>();
                    categories.put("archived", new CategoryData(items, totalFilteredRecords));
                    
                    // Sayfalama bilgileri oluştur
                    int totalPages = totalFilteredRecords > 0 
                            ? (int) Math.ceil((double) totalFilteredRecords / pageSize) 
                            : 0;
                    
                    boolean hasMore = totalFilteredRecords > page * pageSize;
                    
                    PaginationInfo paginationInfo = PaginationInfo.builder()
                            .currentPage(page)
                            .totalPages(totalPages)
                            .pageSize(pageSize)
                            .totalItems(totalFilteredRecords)
                            .hasMore(hasMore)
                            .build();
                    
                    // Yanıtı oluştur
                    PaginatedChatHistoryResponse response = PaginatedChatHistoryResponse.builder()
                            .categories(categories)
                            .pagination(paginationInfo)
                            .build();
                    
                    return Mono.just(response);
                });
    }

    /**
     * ChatHistory listeleme metodlarına arşiv durumu filtresi ekler
     * @param histories Filtre uygulanacak sohbet geçmişi listesi
     * @param showArchived Arşivlenmiş sohbetleri göster (null ise tümünü gösterir)
     * @return Filtre uygulanmış liste
     */
    private List<ChatHistory> filterByArchiveStatus(List<ChatHistory> histories, Boolean showArchived) {
        if (showArchived == null) {
            return histories; // Filtre yok, tümünü göster
        }
        
        return histories.stream()
                .filter(history -> showArchived == !history.isEnable())
                .collect(Collectors.toList());
    }

    public Mono<PaginatedChatHistoryResponse> getChatHistoriesByUserIdCategorized(
            String userId, int page, int pageSize, List<String> categoryFilters, 
            String searchQuery, String sortBy, String sortOrder, Boolean showArchived) { 
        
        // If no categories are specified, use all categories in correct order
        final List<String> finalCategoryFilters = categoryFilters == null || categoryFilters.isEmpty() 
                ? List.of("today", "yesterday", "lastWeek", "lastMonth", "older")
                : categoryFilters;
        
        // Sayfalama mantığını doğru hesaplamak için önce tüm kayıtları getirelim
        return chatHistoryRepository.findAllByUserId(userId, 1, Integer.MAX_VALUE) // Get all histories first
                .collectList()
                .flatMap(allHistories -> {
                    // Arşiv durumuna göre filtrele
                    List<ChatHistory> archiveFilteredHistories = filterByArchiveStatus(allHistories, showArchived);
                    
                    // Veritabanındaki toplam kayıt sayısı (filtrelemeden sonce)
                    int totalDatabaseRecords = archiveFilteredHistories.size(); 
                    
                    // Apply search filter if specified
                    List<ChatHistory> filteredHistories = archiveFilteredHistories;
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String query = searchQuery.toLowerCase();
                        filteredHistories = archiveFilteredHistories.stream()
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
                    
                    // Her kategori için doğru toplam değerleri hesapla
                    int totalFilteredRecords = 0;
                    
                    // Kategori boyutlarını ayrı bir map'te topla - bu sayede doğru toplam değerleri hesaplanacak
                    Map<String, Integer> categoryTotals = new HashMap<>();
                    for (String category : finalCategoryFilters) {
                        int size = categorizedHistories.getOrDefault(category, Collections.emptyList()).size();
                        categoryTotals.put(category, size);
                        totalFilteredRecords += size;
                    }
                     
                    
                    // Sayfalama için skip ve limit değerlerini hesapla
                    int skipCount = (page - 1) * pageSize;
                    int remainingItems = Math.min(pageSize, totalFilteredRecords - skipCount);
                    
                    // Kategorileri işle ve gösterilecek öğeleri belirle
                    if (remainingItems > 0) {
                        for (String category : finalCategoryFilters) {
                            List<ChatHistory> histories = categorizedHistories.getOrDefault(category, Collections.emptyList());
                            int categorySize = categoryTotals.get(category);
                            
                            if (skipCount >= categorySize) {
                                // Bu kategorinin tüm öğelerini atla
                                skipCount -= categorySize;
                                categories.put(category, new CategoryData(Collections.emptyList(), categorySize)); 
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
                                    
                                    remainingItems -= itemsToTake;
                                    skipCount = 0;
                                } else {
                                    categories.put(category, new CategoryData(Collections.emptyList(), categorySize)); 
                                }
                            }
                            
                            if (remainingItems <= 0) {
                                break;
                            }
                        }
                    }
                    
                    // Kalan kategorileri doğru toplam değerleriyle doldur
                    for (String category : finalCategoryFilters) {
                        if (!categories.containsKey(category)) {
                            int categorySize = categoryTotals.getOrDefault(category, 0);
                            categories.put(category, new CategoryData(Collections.emptyList(), categorySize)); 
                        }
                    }
                    
                    // Sayfalama bilgilerini hesapla - toplam filtrelenmiş kayıt sayısını kullan
                    int totalPages = totalFilteredRecords > 0 
                        ? (int) Math.ceil((double) totalFilteredRecords / pageSize) 
                        : 0;
                    
                    // hasMore değerini filtrelenmiş kayıt sayısına göre hesapla
                    boolean hasMore = totalFilteredRecords > page * pageSize;
                     
                    
                    PaginationInfo paginationInfo = PaginationInfo.builder()
                            .currentPage(page)
                            .totalPages(totalPages)
                            .pageSize(pageSize)
                            .totalItems(totalFilteredRecords)
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
        // Güncel UTC zamanını al
        Instant nowInstant = Instant.now();
        LocalDate today = LocalDate.ofInstant(nowInstant, ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
         
        
        // Son hafta ve son ay aralıkları
        LocalDate lastWeekStart = today.minusDays(7); // 7 gün öncesi
        LocalDate lastMonthStart = today.minusDays(30); // 30 gün öncesi
        
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
            
            if (timestamp == null) {
                // Zaman damgası yoksa bugüne ekle ve devam et
                categorized.get("today").add(history); 
                continue;
            }
            
            // Timestamp epoch değerini direkt al ve logla
            long epochSeconds = timestamp.getSeconds();
            
            // Timestamp'i direkt Instant ve sonra LocalDate'e dönüştür
            Instant historyInstant = Instant.ofEpochSecond(epochSeconds);
            LocalDate historyDate = LocalDate.ofInstant(historyInstant, ZoneOffset.UTC); 
            
            // Karşılaştırma yaparken doğrudan tarihleri karşılaştır
            if (historyDate.isEqual(today)) {
                categorized.get("today").add(history); 
            } 
            else if (historyDate.isEqual(yesterday)) {
                categorized.get("yesterday").add(history); 
            }
            // lastWeek: dün ve bugün hariç son 7 gün
            else if (historyDate.isAfter(lastWeekStart) && historyDate.isBefore(yesterday)) {
                categorized.get("lastWeek").add(history); 
            }
            // lastMonth: son hafta hariç son 30 gün
            else if (historyDate.isAfter(lastMonthStart) && historyDate.isBefore(lastWeekStart)) {
                categorized.get("lastMonth").add(history); 
            }
            // 30 günden daha eski
            else {
                categorized.get("older").add(history); 
            }
        }
          
        
        return categorized;
    }

    private LocalDate getLocalDateFromTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return LocalDate.now(ZoneOffset.UTC); // Default to today UTC if no timestamp
        }
        
        // Firestore Timestamp değerlerini UTC olarak değerlendir
        LocalDate date = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC
        ).toLocalDate();
        
        return date;
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

    /**
     * ChatGPT benzeri düz liste halinde sohbet geçmişlerini döndüren metot
     */
    public Mono<Map<String, Object>> getFlatChatHistoriesByUserId(String userId, int offset, int limit, String order, Boolean showArchived) { 
        
        // Tüm kayıtları getir ve sonra filtreleme, sıralama yap
        return chatHistoryRepository.findAllByUserId(userId, 1, Integer.MAX_VALUE)
                .collectList()
                .flatMap(allHistories -> {
                    // Arşiv durumuna göre filtreleme
                    List<ChatHistory> filteredHistories = filterByArchiveStatus(allHistories, showArchived);
                    
                    // Toplam kayıt sayısı
                    int totalCount = filteredHistories.size();
                    
                    // Sıralama kriterleri
                    Comparator<ChatHistory> comparator;
                    if ("created".equals(order)) {
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getCreatedAt()));
                    } else {
                        // Default to updatedAt
                        comparator = Comparator.comparing(history -> getTimestampValue(history.getUpdatedAt()));
                    }
                    
                    // Varsayılan olarak azalan sıralama (en son güncellenen en üstte)
                    comparator = comparator.reversed();
                    
                    // Sıralama ve sayfalama uygula
                    List<Map<String, Object>> items = filteredHistories.stream()
                            .sorted(comparator)
                            .skip(offset)
                            .limit(limit)
                            .map(this::convertToChatGPTFormat)
                            .collect(Collectors.toList());
                    
                    // ChatGPT benzeri yanıt formatı oluştur
                    Map<String, Object> response = new HashMap<>();
                    response.put("items", items);
                    response.put("total", totalCount);
                    response.put("limit", limit);
                    response.put("offset", offset);
                    
                    return Mono.just(response);
                });
    }

    /**
     * ChatHistory modelini ChatGPT benzeri formata dönüştürür
     */
    private Map<String, Object> convertToChatGPTFormat(ChatHistory history) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", history.getId());
        item.put("title", history.getTitle());
        
        // Timestamp formatlarını ChatGPT formatına dönüştür
        if (history.getCreatedAt() != null) {
            // Google Cloud Timestamp'i ISO formatına dönüştür
            Instant createdInstant = Instant.ofEpochSecond(
                    history.getCreatedAt().getSeconds(), 
                    history.getCreatedAt().getNanos());
            item.put("create_time", createdInstant.toString());
        }
        
        if (history.getUpdatedAt() != null) {
            Instant updatedInstant = Instant.ofEpochSecond(
                    history.getUpdatedAt().getSeconds(), 
                    history.getUpdatedAt().getNanos());
            item.put("update_time", updatedInstant.toString());
        }
        
        // Arşivlenme durumu
        item.put("is_archived", !history.isEnable());
        
        // Son konuşma içeriğinden snippet oluştur
        String snippet = null;
        if (history.getConversations() != null && !history.getConversations().isEmpty()) {
            Optional<Conversation> lastConv = history.getConversations().stream()
                    .max(Comparator.comparing(Conversation::getOrderIndex));
            
            if (lastConv.isPresent()) {
                String content = lastConv.get().getContent();
                if (content != null && !content.isEmpty()) {
                    snippet = content.length() > 100 ? content.substring(0, 97) + "..." : content;
                }
            }
        }
        item.put("snippet", snippet);
        
        return item;
    }
}

