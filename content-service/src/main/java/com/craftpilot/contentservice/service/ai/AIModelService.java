package com.craftpilot.contentservice.service.ai;

import com.craftpilot.contentservice.model.Content;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIModelService {
    Mono<String> generateContent(Content content);
    Mono<String> improveContent(Content content);
    Mono<String> analyzeContent(Content content);
    Mono<List<String>> generateSuggestions(Content content);
    String getModelName();
} 