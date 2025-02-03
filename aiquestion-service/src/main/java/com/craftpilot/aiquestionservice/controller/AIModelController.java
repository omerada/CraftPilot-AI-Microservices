package com.craftpilot.aiquestionservice.controller;

import com.craftpilot.aiquestionservice.controller.dto.AIModelRequest;
import com.craftpilot.aiquestionservice.controller.dto.AIModelResponse;
import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.enums.ModelType;
import com.craftpilot.aiquestionservice.service.AIModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai-models")
@RequiredArgsConstructor
@Tag(name = "AI Models", description = "AI model management endpoints")
public class AIModelController {

    private final AIModelService aiModelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new AI model", description = "Create a new AI model with specified parameters")
    public Mono<AIModelResponse> createModel(@RequestBody AIModelRequest request) {
        AIModel model = AIModel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .provider(request.getProvider())
                .version(request.getVersion())
                .endpoint(request.getEndpoint())
                .apiKey(request.getApiKey())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .isActive(request.getIsActive())
                .build();

        return aiModelService.createModel(model)
                .map(AIModelResponse::fromModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get AI model by ID", description = "Retrieve a specific AI model by its ID")
    public Mono<AIModelResponse> getModel(@PathVariable String id) {
        return aiModelService.getModel(id)
                .map(AIModelResponse::fromModel);
    }

    @GetMapping
    @Operation(summary = "Get all active AI models", description = "Retrieve all active AI models")
    public Flux<AIModelResponse> getAllModels() {
        return aiModelService.getAllModels()
                .map(AIModelResponse::fromModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update AI model", description = "Update an existing AI model")
    public Mono<AIModelResponse> updateModel(@PathVariable String id, @RequestBody AIModelRequest request) {
        AIModel model = AIModel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .provider(request.getProvider())
                .version(request.getVersion())
                .endpoint(request.getEndpoint())
                .apiKey(request.getApiKey())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .isActive(request.getIsActive())
                .build();

        return aiModelService.updateModel(id, model)
                .map(AIModelResponse::fromModel);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete AI model", description = "Delete a specific AI model by its ID")
    public Mono<Void> deleteModel(@PathVariable String id) {
        return aiModelService.deleteModel(id);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get active AI model by type", description = "Retrieve an active AI model by its type")
    public Mono<AIModelResponse> getActiveModelByType(@PathVariable ModelType type) {
        return aiModelService.getActiveModelByType(type)
                .map(AIModelResponse::fromModel);
    }
} 