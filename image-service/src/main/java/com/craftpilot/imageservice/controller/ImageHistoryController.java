package com.craftpilot.imageservice.controller;

import com.craftpilot.imageservice.model.ImageHistory;
import com.craftpilot.imageservice.service.ImageHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/image-histories")
@RequiredArgsConstructor
@Tag(name = "Image History", description = "Image history management endpoints")
public class ImageHistoryController {
    private final ImageHistoryService imageHistoryService;

    @PostMapping
    @Operation(summary = "Save image history", description = "Saves a new image history or updates an existing one")
    public Mono<ResponseEntity<ImageHistory>> saveImageHistory(@RequestBody ImageHistory imageHistory) {
        return imageHistoryService.saveImageHistory(imageHistory)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image history by ID", description = "Retrieves image history by its unique identifier")
    public Mono<ResponseEntity<ImageHistory>> getImageHistory(@PathVariable String id) {
        return imageHistoryService.getImageHistory(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's image histories", description = "Retrieves the last 5 image histories for a specific user")
    public Flux<ImageHistory> getUserImageHistories(@PathVariable String userId) {
        return imageHistoryService.getUserImageHistories(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete image history", description = "Deletes image history by its unique identifier")
    public Mono<ResponseEntity<Void>> deleteImageHistory(@PathVariable String id) {
        return imageHistoryService.deleteImageHistory(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
} 