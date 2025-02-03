package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.Content; 
import com.craftpilot.contentservice.model.dto.ContentRequest;
import com.craftpilot.contentservice.model.dto.ContentResponse;
import com.craftpilot.contentservice.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid; 

@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Content management APIs")
@SecurityRequirement(name = "bearer-key")
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new content")
    public Mono<ContentResponse> createContent(
            @Parameter(hidden = true) @AuthenticationPrincipal String userId,
            @Valid @RequestBody ContentRequest request) {
        return contentService.createContent(userId, request)
                .map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by ID")
    public Mono<ContentResponse> getContent(
            @Parameter(description = "Content ID") @PathVariable String id) {
        return contentService.getContent(id)
                .map(this::mapToResponse);
    }

    @GetMapping
    @Operation(summary = "Get all contents")
    public Flux<ContentResponse> getAllContents() {
        return contentService.getAllContents()
                .map(this::mapToResponse);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get contents by type")
    public Flux<ContentResponse> getContentsByType(
            @Parameter(description = "Content type") @PathVariable String type) {
        return contentService.getContentsByType(type)
                .map(this::mapToResponse);
    }

    @GetMapping("/search")
    @Operation(summary = "Search contents")
    public Flux<ContentResponse> searchContents(
            @Parameter(description = "Search query") @RequestParam String query) {
        return contentService.searchContents(query)
                .map(this::mapToResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content")
    public Mono<ContentResponse> updateContent(
            @Parameter(description = "Content ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal String userId,
            @Valid @RequestBody ContentRequest request) {
        return contentService.updateContent(id, userId, request)
                .map(this::mapToResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete content")
    public Mono<Void> deleteContent(
            @Parameter(description = "Content ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal String userId) {
        return contentService.deleteContent(id, userId);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish content")
    public Mono<ContentResponse> publishContent(
            @Parameter(description = "Content ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal String userId) {
        return contentService.publishContent(id, userId)
                .map(this::mapToResponse);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive content")
    public Mono<ContentResponse> archiveContent(
            @Parameter(description = "Content ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal String userId) {
        return contentService.archiveContent(id, userId)
                .map(this::mapToResponse);
    }

    @PostMapping("/{id}/improve")
    @Operation(summary = "Improve content using AI")
    public Mono<ContentResponse> improveContent(
            @Parameter(description = "Content ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal String userId) {
        return contentService.improveContent(id, userId)
                .map(this::mapToResponse);
    }

    private ContentResponse mapToResponse(Content content) {
        return ContentResponse.builder()
                .id(content.getId())
                .userId(content.getUserId())
                .title(content.getTitle())
                .description(content.getDescription())
                .content(content.getContent())
                .type(content.getType())
                .status(content.getStatus())
                .tags(content.getTags())
                .metadata(content.getMetadata())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .createdBy(content.getCreatedBy())
                .updatedBy(content.getUpdatedBy())
                .version(content.getVersion())
                .build();
    }
} 