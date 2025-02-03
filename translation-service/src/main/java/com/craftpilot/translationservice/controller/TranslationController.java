package com.craftpilot.translationservice.controller;

import com.craftpilot.translationservice.model.Translation;
import com.craftpilot.translationservice.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; 
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono; 

@RestController
@RequestMapping("/translations")
@RequiredArgsConstructor
@Tag(name = "Translation", description = "Translation management endpoints")
public class TranslationController {
    private final TranslationService translationService;

    @PostMapping
    @Operation(summary = "Create translation", description = "Creates a new translation")
    public Mono<Translation> createTranslation(@RequestBody Translation translation) {
        return translationService.createTranslation(translation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get translation by ID", description = "Retrieves translation by its unique identifier")
    public Mono<Translation> getTranslation(@PathVariable String id) {
        return translationService.getTranslation(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's translations", description = "Retrieves all translations created by a specific user")
    public Flux<Translation> getTranslationsByUserId(@PathVariable String userId) {
        return translationService.getTranslationsByUserId(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete translation", description = "Deletes translation by its unique identifier")
    public Mono<Void> deleteTranslation(@PathVariable String id) {
        return translationService.deleteTranslation(id);
    }
} 