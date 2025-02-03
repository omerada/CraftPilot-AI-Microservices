package com.craftpilot.imageservice.controller;

import com.craftpilot.imageservice.model.Image;
import com.craftpilot.imageservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "Image generation and management endpoints")
public class ImageController {
    private final ImageService imageService;

    @PostMapping
    @Operation(summary = "Generate image", description = "Generates an image based on the provided prompt and settings")
    public Mono<ResponseEntity<Image>> generateImage(@RequestBody Image image) {
        return imageService.generateImage(image)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image by ID", description = "Retrieves image by its unique identifier")
    public Mono<ResponseEntity<Image>> getImage(@PathVariable String id) {
        return imageService.getImage(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's images", description = "Retrieves all images created by a specific user")
    public Flux<Image> getUserImages(@PathVariable String userId) {
        return imageService.getUserImages(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete image", description = "Deletes image by its unique identifier")
    public Mono<ResponseEntity<Void>> deleteImage(@PathVariable String id) {
        return imageService.deleteImage(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
} 