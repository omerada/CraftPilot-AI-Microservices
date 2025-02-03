package com.craftpilot.aiquestionservice.service;

import com.craftpilot.aiquestionservice.exception.AIModelNotFoundException;
import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.enums.ModelType;
import com.craftpilot.aiquestionservice.repository.FirestoreAIModelRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelService {
    private final FirestoreAIModelRepository aiModelRepository;

    public Mono<AIModel> createModel(AIModel model) {
        if (model.getId() == null) {
            model.setId(UUID.randomUUID().toString());
        }

        if (model.getCreatedAt() == null) {
            model.setCreatedAt(Timestamp.now());
        }
        model.setUpdatedAt(Timestamp.now());

        return aiModelRepository.save(model)
                .doOnSuccess(savedModel -> log.info("AI model created successfully with ID: {}", savedModel.getId()))
                .doOnError(error -> log.error("Error creating AI model", error));
    }

    public Mono<AIModel> getModel(String id) {
        return aiModelRepository.findById(id)
                .doOnSuccess(model -> {
                    if (model != null) {
                        log.info("AI model found with ID: {}", id);
                    } else {
                        log.info("No AI model found with ID: {}", id);
                    }
                })
                .doOnError(error -> log.error("Error retrieving AI model with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new AIModelNotFoundException("AI model not found with ID: " + id)));
    }

    public Mono<AIModel> updateModel(String id, AIModel model) {
        model.setId(id);
        return aiModelRepository.findById(id)
                .flatMap(existingModel -> {
                    model.setCreatedAt(existingModel.getCreatedAt());
                    model.setUpdatedAt(Timestamp.now());
                    return aiModelRepository.save(model);
                })
                .doOnSuccess(updatedModel -> log.info("AI model updated successfully with ID: {}", id))
                .doOnError(error -> log.error("Error updating AI model with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new AIModelNotFoundException("AI model not found with ID: " + id)));
    }

    public Mono<Void> deleteModel(String id) {
        return aiModelRepository.findById(id)
                .flatMap(model -> aiModelRepository.deleteById(id))
                .doOnSuccess(result -> log.info("AI model deleted successfully with ID: {}", id))
                .doOnError(error -> log.error("Error deleting AI model with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new AIModelNotFoundException("AI model not found with ID: " + id)));
    }

    public Flux<AIModel> getAllModels() {
        return aiModelRepository.findAll()
                .doOnComplete(() -> log.info("Retrieved all AI models"))
                .doOnError(error -> log.error("Error retrieving all AI models", error));
    }

    public Mono<AIModel> getActiveModelByType(ModelType type) {
        return aiModelRepository.findByTypeAndIsActiveTrue(type)
                .doOnSuccess(model -> {
                    if (model != null) {
                        log.info("Active AI model found with type: {}", type);
                    } else {
                        log.info("No active AI model found with type: {}", type);
                    }
                })
                .doOnError(error -> log.error("Error retrieving active AI model with type: {}", type, error))
                .switchIfEmpty(Mono.error(new AIModelNotFoundException("No active AI model found with type: " + type)));
    }
} 