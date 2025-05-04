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

    @GetMapping("/{userId}")
    @Operation(summary = "Get user memory", description = "Retrieves memory for a specific user")
    public Mono<ResponseEntity<UserMemory>> getUserMemory(@PathVariable String userId) {
        return userMemoryService.getUserMemory(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/entry")
    @Operation(summary = "Add memory entry", description = "Adds a new memory entry for a specific user")
    public Mono<ResponseEntity<UserMemory>> addMemoryEntry(
            @PathVariable String userId,
            @RequestBody UserMemory.MemoryEntry entry) {
        return userMemoryService.addMemoryEntry(userId, entry)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Update user memory", description = "Updates the entire memory for a specific user")
    public Mono<ResponseEntity<UserMemory>> updateUserMemory(@RequestBody UserMemory userMemory) {
        return userMemoryService.updateUserMemory(userMemory)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/clean")
    @Operation(summary = "Clean old entries", description = "Removes entries older than the specified threshold")
    public Mono<ResponseEntity<UserMemory>> cleanOldEntries(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int daysThreshold) {
        return userMemoryService.cleanOldEntries(userId, daysThreshold)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
