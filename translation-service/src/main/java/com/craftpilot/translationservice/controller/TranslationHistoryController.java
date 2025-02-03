package com.craftpilot.translationservice.controller;

import com.craftpilot.translationservice.model.TranslationHistory;
import com.craftpilot.translationservice.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/translation-histories")
@RequiredArgsConstructor
@Tag(name = "Translation History", description = "Translation history management endpoints")
public class TranslationHistoryController {
    private final TranslationService translationService;

    @PostMapping
    @Operation(summary = "Create translation history", description = "Creates a new translation history record")
    public Mono<TranslationHistory> createTranslationHistory(@RequestBody TranslationHistory history) {
        return translationService.createTranslationHistory(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get translation history by ID", description = "Retrieves translation history by its unique identifier")
    public Mono<TranslationHistory> getTranslationHistory(@PathVariable String id) {
        return translationService.getTranslationHistory(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's translation histories", description = "Retrieves all translation histories created by a specific user")
    public Flux<TranslationHistory> getTranslationHistoriesByUserId(@PathVariable String userId) {
        return translationService.getTranslationHistoriesByUserId(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete translation history", description = "Deletes translation history by its unique identifier")
    public Mono<Void> deleteTranslationHistory(@PathVariable String id) {
        return translationService.deleteTranslationHistory(id);
    }
} 