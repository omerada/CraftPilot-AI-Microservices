package com.craftpilot.aiquestionservice.service.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AIModelFactory {
    private final Map<String, AIModelService> modelServices;

    public AIModelFactory(List<AIModelService> services) {
        this.modelServices = services.stream()
                .collect(Collectors.toMap(AIModelService::getModelName, Function.identity()));
    }

    public AIModelService getModel(String modelName) {
        AIModelService service = modelServices.get(modelName);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported AI model: " + modelName);
        }
        return service;
    }
} 