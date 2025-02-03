package com.craftpilot.modelservice.service;

import com.craftpilot.modelservice.event.ModelEvent;
import com.craftpilot.modelservice.model.AIModel;
import com.craftpilot.modelservice.repository.AIModelRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelService {
    private final AIModelRepository modelRepository;
    private final KafkaTemplate<String, ModelEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Cacheable(value = "models", key = "#id")
    public Mono<AIModel> getModel(String id) {
        return modelRepository.findById(id)
                .doOnSuccess(model -> {
                    if (model != null) {
                        log.debug("Retrieved model: {}", id);
                        meterRegistry.counter("model.retrieval.success").increment();
                    } else {
                        log.debug("Model not found: {}", id);
                        meterRegistry.counter("model.retrieval.notfound").increment();
                    }
                });
    }

    public Flux<AIModel> getAllModels() {
        return modelRepository.findAll()
                .doOnComplete(() -> {
                    log.debug("Retrieved all models");
                    meterRegistry.counter("model.retrieval.all").increment();
                });
    }

    public Flux<AIModel> getModelsByType(AIModel.ModelType type) {
        return modelRepository.findByType(type)
                .doOnComplete(() -> {
                    log.debug("Retrieved models by type: {}", type);
                    meterRegistry.counter("model.retrieval.bytype").increment();
                });
    }

    public Flux<AIModel> getModelsByProvider(String provider) {
        return modelRepository.findByProvider(provider)
                .doOnComplete(() -> {
                    log.debug("Retrieved models by provider: {}", provider);
                    meterRegistry.counter("model.retrieval.byprovider").increment();
                });
    }

    public Mono<AIModel> createModel(AIModel model) {
        return modelRepository.save(model)
                .doOnSuccess(savedModel -> {
                    log.info("Created model: {}", savedModel.getId());
                    meterRegistry.counter("model.creation").increment();
                    sendModelEvent("MODEL_CREATED", savedModel);
                });
    }

    @CacheEvict(value = "models", key = "#id")
    public Mono<AIModel> updateModel(String id, AIModel model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    model.setId(id);
                    model.setCreatedAt(existingModel.getCreatedAt());
                    model.setUpdatedAt(LocalDateTime.now());
                    return modelRepository.save(model);
                })
                .doOnSuccess(updatedModel -> {
                    log.info("Updated model: {}", id);
                    meterRegistry.counter("model.update").increment();
                    sendModelEvent("MODEL_UPDATED", updatedModel);
                });
    }

    @CacheEvict(value = "models", key = "#id")
    public Mono<Void> deleteModel(String id) {
        return modelRepository.deleteById(id)
                .doOnSuccess(unused -> {
                    log.info("Deleted model: {}", id);
                    meterRegistry.counter("model.deletion").increment();
                    sendModelEvent("MODEL_DELETED", AIModel.builder().id(id).build());
                });
    }

    public Mono<AIModel> updateModelMetrics(String id, Map<String, Object> metrics) {
        return modelRepository.findById(id)
                .flatMap(model -> {
                    model.setMetrics(metrics);
                    return modelRepository.save(model);
                })
                .doOnSuccess(updatedModel -> {
                    log.info("Updated metrics for model: {}", id);
                    meterRegistry.counter("model.metrics.update").increment();
                    sendModelEvent("MODEL_METRICS_UPDATED", updatedModel);
                });
    }

    public Mono<AIModel> updateModelStatus(String id, AIModel.ModelStatus status) {
        return modelRepository.findById(id)
                .flatMap(model -> {
                    model.setStatus(status);
                    return modelRepository.save(model);
                })
                .doOnSuccess(updatedModel -> {
                    log.info("Updated status for model: {} to {}", id, status);
                    meterRegistry.counter("model.status.update").increment();
                    sendModelEvent("MODEL_STATUS_UPDATED", updatedModel);
                });
    }

    private void sendModelEvent(String eventType, AIModel model) {
        ModelEvent event = ModelEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .modelId(model.getId())
                .modelType(model.getType())
                .status(model.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("model-events", event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Sent model event: {}", eventType);
                    } else {
                        log.error("Error sending model event: {}", ex.getMessage());
                    }
                });
    }
} 