package com.craftpilot.usermemoryservice.controller;

import com.craftpilot.usermemoryservice.model.UserMemory;
import com.craftpilot.usermemoryservice.service.UserMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user-memory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Memory", description = "User memory management endpoints")
public class UserMemoryController {
    
    private final UserMemoryService userMemoryService;
    
    @PostMapping("/{userId}/process")
    @Operation(summary = "Process user message", description = "Process a user message for memory extraction")
    public Mono<Void> processUserMessage(@PathVariable String userId, @RequestBody String message) {
        log.info("Processing message for user: {}", userId);
        return userMemoryService.processUserMessage(userId, message);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user memory", description = "Retrieve the memory for a specific user")
    public Mono<ResponseEntity<UserMemory>> getUserMemory(@PathVariable String userId) {
        log.info("Retrieving memory for user: {}", userId);
        return userMemoryService.getUserMemory(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
