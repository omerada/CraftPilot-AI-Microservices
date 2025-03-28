package com.craftpilot.llmservice.controller;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import com.craftpilot.llmservice.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    @GetMapping("/histories")
    public Flux<ChatHistory> getChatHistories(@RequestParam String userId) {
        return chatHistoryService.getChatHistoriesByUserId(userId);
    }

    @GetMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> getChatHistoryById(@PathVariable String id) {
        return chatHistoryService.getChatHistoryById(id)
                .map(history -> ResponseEntity.ok(history))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/histories")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatHistory> createChatHistory(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.createChatHistory(chatHistory);
    }

    @PutMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistory(@PathVariable String id, @RequestBody ChatHistory chatHistory) {
        chatHistory.setId(id);
        return chatHistoryService.updateChatHistory(chatHistory)
                .map(updated -> ResponseEntity.ok(updated))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/histories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChatHistory(@PathVariable String id) {
        return chatHistoryService.deleteChatHistory(id);
    }

    @PostMapping("/histories/{id}/conversations")
    public Mono<ResponseEntity<ChatHistory>> addConversation(@PathVariable String id, @RequestBody Conversation conversation) {
        return chatHistoryService.addConversation(id, conversation)
                .map(updated -> ResponseEntity.ok(updated))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/histories/{id}")
    public Mono<ResponseEntity<ChatHistory>> updateChatHistoryTitle(@PathVariable String id, @RequestBody TitleUpdateRequest request) {
        return chatHistoryService.updateChatHistoryTitle(id, request.getTitle())
                .map(updated -> ResponseEntity.ok(updated))
                .defaultIfEmpty(ResponseEntity.notFound().build());
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
}
