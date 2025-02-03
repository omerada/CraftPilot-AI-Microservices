package com.craftpilot.codeservice.controller;

import com.craftpilot.codeservice.model.CodeHistory;
import com.craftpilot.codeservice.service.CodeHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/code-histories")
@RequiredArgsConstructor
@Tag(name = "Code History", description = "Code history management endpoints")
public class CodeHistoryController {
    private final CodeHistoryService codeHistoryService;

    @PostMapping
    @Operation(summary = "Save code history", description = "Saves a new code history or updates an existing one")
    public Mono<ResponseEntity<CodeHistory>> saveCodeHistory(@RequestBody CodeHistory codeHistory) {
        return codeHistoryService.saveCodeHistory(codeHistory)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get code history by ID", description = "Retrieves code history by its unique identifier")
    public Mono<ResponseEntity<CodeHistory>> getCodeHistory(@PathVariable String id) {
        return codeHistoryService.getCodeHistory(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's code histories", description = "Retrieves the last 5 code histories for a specific user")
    public Flux<CodeHistory> getUserCodeHistories(@PathVariable String userId) {
        return codeHistoryService.getUserCodeHistories(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete code history", description = "Deletes code history by its unique identifier")
    public Mono<ResponseEntity<Void>> deleteCodeHistory(@PathVariable String id) {
        return codeHistoryService.deleteCodeHistory(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
} 