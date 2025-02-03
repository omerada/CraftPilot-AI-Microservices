package com.craftpilot.aiquestionservice.service.ai;

import com.craftpilot.aiquestionservice.model.Question;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIModelService {
    Mono<String> generateAnswer(Question question);
    Mono<List<String>> generateRelatedQuestions(Question question);
    String getModelName();
} 