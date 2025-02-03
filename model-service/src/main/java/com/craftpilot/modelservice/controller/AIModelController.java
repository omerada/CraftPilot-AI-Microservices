package com.craftpilot.modelservice.controller;

import com.craftpilot.modelservice.model.AIModel;
import com.craftpilot.modelservice.service.AIModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "AI Model", description = "AI model management APIs")
public class AIModelController {
    private final AIModelService modelService;

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID")
    public Mono<ResponseEntity<AIModel>> getModel(@PathVariable String id) {
        return modelService.getModel(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all models")
    public Flux<AIModel> getAllModels() {
        return modelService.getAllModels();
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get models by type")
    public Flux<AIModel> getModelsByType(@PathVariable AIModel.ModelType type) {
        return modelService.getModelsByType(type);
    }

    @GetMapping("/provider/{provider}")
    @Operation(summary = "Get models by provider")
    public Flux<AIModel> getModelsByProvider(@PathVariable String provider) {
        return modelService.getModelsByProvider(provider);
    }

    @PostMapping
    @Operation(summary = "Create a new model")
    public Mono<ResponseEntity<AIModel>> createModel(@RequestBody AIModel model) {
        return modelService.createModel(model)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model by ID")
    public Mono<ResponseEntity<AIModel>> updateModel(
            @PathVariable String id,
            @RequestBody AIModel model) {
        return modelService.updateModel(id, model)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model by ID")
    public Mono<ResponseEntity<Void>> deleteModel(@PathVariable String id) {
        return modelService.deleteModel(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PutMapping("/{id}/metrics")
    @Operation(summary = "Update model metrics")
    public Mono<ResponseEntity<AIModel>> updateModelMetrics(
            @PathVariable String id,
            @RequestBody Map<String, Object> metrics) {
        return modelService.updateModelMetrics(id, metrics)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update model status")
    public Mono<ResponseEntity<AIModel>> updateModelStatus(
            @PathVariable String id,
            @RequestBody AIModel.ModelStatus status) {
        return modelService.updateModelStatus(id, status)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
} 