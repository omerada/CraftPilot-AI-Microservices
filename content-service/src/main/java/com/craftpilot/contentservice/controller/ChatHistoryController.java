package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.ChatHistory;
import com.craftpilot.contentservice.model.ChatMessage;
import com.craftpilot.contentservice.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; 
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/chat-histories")
@RequiredArgsConstructor
@Tag(name = "Chat History", description = "Chat history management APIs")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatHistory> createChatHistory(@RequestParam String userId, @RequestParam String contentId) {
        return chatHistoryService.createChatHistory(userId, contentId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chat history by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chat history found"),
        @ApiResponse(responseCode = "404", description = "Chat history not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ChatHistory> getChatHistory(@PathVariable String id) {
        return chatHistoryService.getChatHistory(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get chat history by user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chat history found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Flux<ChatHistory> getChatHistoriesByUserId(@PathVariable String userId) {
        return chatHistoryService.getChatHistoriesByUserId(userId);
    }

    @PostMapping("/{id}/messages")
    public Mono<ChatHistory> addMessage(@PathVariable String id, @Valid @RequestBody ChatMessage message) {
        return chatHistoryService.addMessage(id, message);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete chat history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Chat history deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Chat history not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChatHistory(@PathVariable String id) {
        return chatHistoryService.deleteChatHistory(id);
    }
} 