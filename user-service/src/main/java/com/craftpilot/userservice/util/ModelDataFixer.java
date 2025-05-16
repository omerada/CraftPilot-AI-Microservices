package com.craftpilot.userservice.util;

import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import com.craftpilot.userservice.repository.AIModelRepository;
import com.craftpilot.userservice.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Utility class to fix model data structure issues.
 * Only runs when the "fix-models" profile is active.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("fix-models")
public class ModelDataFixer implements CommandLineRunner {

    private final ReactiveMongoTemplate mongoTemplate;
    private final AIModelRepository modelRepository;
    private final ProviderRepository providerRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Running model data fixer...");

        // Fix models with redundant _id and id fields
        fixModelStructure()
            .then(ensureProvidersExist())
            .doOnSuccess(v -> log.info("Model data fixing completed successfully"))
            .doOnError(e -> log.error("Error fixing model data: {}", e.getMessage(), e))
            .block();
    }

    private Mono<Void> fixModelStructure() {
        log.info("Fixing model structure...");
        
        return mongoTemplate.find(new Query(), AIModel.class)
            .flatMap(model -> {
                // Check if this is a model with the old structure
                if (model.getModelId() == null) {
                    log.warn("Model has null modelId: {}", model);
                    return Mono.empty();
                }
                
                // Create a new AIModel with the correct structure
                AIModel fixedModel = new AIModel();
                fixedModel.setModelId(model.getModelId());
                fixedModel.setModelName(model.getModelName());
                fixedModel.setProvider(model.getProvider());
                fixedModel.setMaxInputTokens(model.getMaxInputTokens());
                fixedModel.setRequiredPlan(model.getRequiredPlan());
                fixedModel.setCreditCost(model.getCreditCost());
                fixedModel.setCreditType(model.getCreditType());
                fixedModel.setCategory(model.getCategory());
                fixedModel.setContextLength(model.getContextLength());
                fixedModel.setDefaultTemperature(model.getDefaultTemperature());
                fixedModel.setIcon(model.getIcon());
                fixedModel.setDescription(model.getDescription());
                fixedModel.setFee(model.getFee());
                fixedModel.setFeatured(model.getFeatured());
                fixedModel.setMaxTokens(model.getMaxTokens());
                fixedModel.setMultimodal(model.getMultimodal());
                fixedModel.setActive(model.getActive());
                
                return modelRepository.save(fixedModel)
                    .doOnSuccess(saved -> log.info("Fixed model: {}", saved.getModelId()))
                    .onErrorResume(e -> {
                        log.error("Error fixing model {}: {}", model.getModelId(), e.getMessage());
                        return Mono.empty();
                    });
            })
            .then();
    }

    private Mono<Void> ensureProvidersExist() {
        log.info("Ensuring providers exist...");
        
        // Get all unique provider names from models
        return modelRepository.findAll()
            .map(AIModel::getProvider)
            .filter(Objects::nonNull)
            .distinct()
            .collectList()
            .flatMapMany(providerNames -> {
                log.info("Found {} unique providers in models", providerNames.size());
                
                // For each provider name, ensure a Provider entity exists
                return Flux.fromIterable(providerNames)
                    .flatMap(providerName -> {
                        return providerRepository.findById(providerName)
                            .switchIfEmpty(Mono.defer(() -> {
                                Provider provider = Provider.builder()
                                    .name(providerName)
                                    .description("Provider for " + providerName + " models")
                                    .build();
                                
                                log.info("Creating missing provider: {}", providerName);
                                return providerRepository.save(provider);
                            }));
                    });
            })
            .then();
    }
}
