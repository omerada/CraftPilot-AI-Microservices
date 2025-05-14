package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.craftpilot.llmservice.model.response.CategoryData;
import com.craftpilot.llmservice.model.response.PaginatedChatHistoryResponse;
import com.craftpilot.llmservice.model.response.PaginationInfo;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    @GetMapping("/histories")
    public Mono<ResponseEntity<PaginatedChatHistoryResponse>> getChatHistories(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Boolean showArchived) {

        return chatHistoryService
                .getChatHistoriesByUserIdCategorized(userId, page, pageSize, categories, searchQuery, sortBy, sortOrder,
                        showArchived)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi alınırken hata: {}", error.getMessage());
                    // Return an empty response in case of error
                    PaginatedChatHistoryResponse emptyResponse = createEmptyResponse(page, pageSize);
                    return Mono.just(ResponseEntity.ok(emptyResponse));
                });
    }

    private PaginatedChatHistoryResponse createEmptyResponse(int page, int pageSize) {
        LinkedHashMap<String, CategoryData> emptyCategories = new LinkedHashMap<>();
        // Sıralamayı koruyarak boş kategorileri ekle
        emptyCategories.put("today", new CategoryData(List.of(), 0));
        emptyCategories.put("yesterday", new CategoryData(List.of(), 0));
        emptyCategories.put("lastWeek", new CategoryData(List.of(), 0));
        emptyCategories.put("lastMonth", new CategoryData(List.of(), 0));
        emptyCategories.put("older", new CategoryData(List.of(), 0));

        PaginationInfo pagination = PaginationInfo.builder()
                .currentPage(page)
                .totalPages(0)
                .pageSize(pageSize)
                .totalItems(0)
                .hasMore(false)
                .build();

        return PaginatedChatHistoryResponse.builder()
                .categories(emptyCategories)
                .pagination(pagination)
                .build();
    }

    @GetMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> getChatHistoryById(@PathVariable String id) {

        return chatHistoryService.getChatHistoryById(id)
                .map(history -> ResponseEntity.ok(history))
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi detayı alınırken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/histories")
    public Mono<ResponseEntity<ChatHistory>> createChatHistory(@RequestBody ChatHistory chatHistory) {

        // String veya Long olarak gelen timestamp değerlerini işle
        processTimestamps(chatHistory);

        return chatHistoryService.createChatHistory(chatHistory)
                .map(createdHistory -> {
                    return ResponseEntity.status(HttpStatus.CREATED).body(createdHistory);
                })
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi oluşturulurken hata: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistory(@PathVariable String id,
            @RequestBody ChatHistory chatHistory) {

        chatHistory.setId(id);
        return chatHistoryService.updateChatHistory(chatHistory)
                .map(updated -> ResponseEntity.ok(updated))
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi güncellenirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/histories/{id}")
    public Mono<ResponseEntity<Void>> deleteChatHistory(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {

        return chatHistoryService.deleteChatHistory(userId, id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi silinirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/histories/{id}/conversations")
    public Mono<ResponseEntity<ChatHistory>> addConversation(@PathVariable String id,
            @RequestBody Conversation conversation) {

        // Process timestamp if needed
        processConversationTimestamp(conversation);

        return chatHistoryService.addConversation(id, conversation)
                .map(updated -> {
                    // Mesaj ekleme başarılı oldu, tüm orderIndex'leri logla
                    if (updated != null && updated.getConversations() != null) {
                        StringBuilder sb = new StringBuilder("Güncel mesaj sıralaması - Chat ID: " + id + " - ");
                        for (Conversation c : updated.getConversations()) {
                            sb.append(c.getOrderIndex())
                                    .append("(").append(c.getRole()).append("), ");
                        }
                    }

                    return ResponseEntity.ok(updated);
                })
                .onErrorResume(error -> {
                    log.error("Mesaj eklenirken hata, Chat ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/histories/{id}/update-title")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistoryTitlePost(
            @PathVariable String id,
            @RequestBody TitleUpdateRequest request) {
        return chatHistoryService.getChatHistoryById(id)
                .flatMap(chatHistory -> {
                    chatHistory.setTitle(request.getTitle());
                    chatHistory.setUpdatedAt(LocalDateTime.now());
                    return chatHistoryService.updateChatHistory(chatHistory);
                })
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Sohbet başlığı güncellenirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/histories/{id}/do-archive")
    public Mono<ResponseEntity<ChatHistory>> archiveChatHistoryPost(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {

        return chatHistoryService.archiveChatHistory(userId, id)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Sohbet arşivlenirken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/flat-histories")
    public Mono<ResponseEntity<Map<String, Object>>> getFlatChatHistories(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "updated") String order,
            @RequestParam(required = false) Boolean showArchived) {

        return chatHistoryService.getFlatChatHistoriesByUserId(userId, offset, limit, order, showArchived)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Sohbet geçmişi alınırken hata: {}", error.getMessage());
                    // Boş yanıt dön
                    Map<String, Object> emptyResponse = new HashMap<>();
                    emptyResponse.put("items", List.of());
                    emptyResponse.put("total", 0);
                    emptyResponse.put("limit", limit);
                    emptyResponse.put("offset", offset);
                    return Mono.just(ResponseEntity.ok(emptyResponse));
                });
    }

    public static class TitleUpdateRequest {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Frontend'den gelen timestamp değerlerini LocalDateTime'a dönüştürür
     */
    private void processTimestamps(ChatHistory chatHistory) {
        try {
            // createdAt ve updatedAt alanları null ise şu anki zamanı kullan
            if (chatHistory.getCreatedAt() == null) {
                chatHistory.setCreatedAt(LocalDateTime.now());
            }

            if (chatHistory.getUpdatedAt() == null) {
                chatHistory.setUpdatedAt(LocalDateTime.now());
            }

            // Eğer conversations listesi varsa içindeki timestamp değerlerini de işle
            if (chatHistory.getConversations() != null) {
                chatHistory.getConversations().forEach(this::processConversationTimestamp);
            }
        } catch (Exception e) {
            log.error("Timestamp işlenirken hata: {}", e.getMessage());
            // Hata durumunda yeni timestamp'ler oluştur
            chatHistory.setCreatedAt(LocalDateTime.now());
            chatHistory.setUpdatedAt(LocalDateTime.now());
        }
    }

    /**
     * Conversation için timestamp işleme
     */
    private void processConversationTimestamp(Conversation conversation) {
        try {
            if (conversation.getTimestamp() == null) {
                conversation.setTimestamp(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Conversation timestamp işlenirken hata: {}", e.getMessage());
            conversation.setTimestamp(LocalDateTime.now());
        }
    }

    @PostMapping("/histories/{id}/do-unarchive")
    public Mono<ResponseEntity<ChatHistory>> unarchiveChatHistoryPost(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {

        return chatHistoryService.unarchiveChatHistory(userId, id)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Sohbet arşivden çıkarılırken hata, ID {}: {}", id, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/archived-histories")
    public Mono<ResponseEntity<PaginatedChatHistoryResponse>> getArchivedChatHistories(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        return chatHistoryService.getArchivedChatHistories(userId, page, pageSize, searchQuery, sortBy, sortOrder)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Arşivlenmiş sohbet geçmişi alınırken hata: {}", error.getMessage());
                    // Return an empty response in case of error
                    PaginatedChatHistoryResponse emptyResponse = createEmptyResponse(page, pageSize);
                    return Mono.just(ResponseEntity.ok(emptyResponse));
                });
    }
}
