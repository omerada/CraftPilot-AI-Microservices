package com.craftpilot.notificationservice.controller;

import com.craftpilot.notificationservice.dto.NotificationTemplateRequest;
import com.craftpilot.notificationservice.dto.NotificationTemplateResponse;
import com.craftpilot.notificationservice.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Notification Template Controller", description = "Notification template management endpoints")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new template")
    public Mono<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public Mono<NotificationTemplateResponse> getTemplate(@PathVariable String id) {
        return templateService.getTemplate(id)
                .map(NotificationTemplateResponse::fromEntity);
    }

    @GetMapping
    @Operation(summary = "Get all active templates")
    public Flux<NotificationTemplateResponse> getAllActiveTemplates() {
        return templateService.getAllActiveTemplates();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template")
    public Mono<NotificationTemplateResponse> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        return templateService.updateTemplate(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete template")
    public Mono<Void> deleteTemplate(@PathVariable String id) {
        return templateService.deleteTemplate(id);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate template")
    public Mono<NotificationTemplateResponse> activateTemplate(@PathVariable String id) {
        return templateService.activateTemplate(id);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate template")
    public Mono<NotificationTemplateResponse> deactivateTemplate(@PathVariable String id) {
        return templateService.deactivateTemplate(id);
    }
} 