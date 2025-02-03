package com.craftpilot.translationservice.service;

import com.craftpilot.translationservice.model.TranslationHistory;
import com.craftpilot.translationservice.repository.TranslationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TranslationHistoryService {

    private final TranslationHistoryRepository translationHistoryRepository;

    public Mono<TranslationHistory> createTranslationHistory(TranslationHistory history) {
        return translationHistoryRepository.save(history);
    }

    public Mono<TranslationHistory> getTranslationHistory(String id) {
        return translationHistoryRepository.findById(id);
    }

    public Flux<TranslationHistory> getTranslationHistoriesByUserId(String userId) {
        return translationHistoryRepository.findByUserId(userId);
    }

    public Mono<Void> deleteTranslationHistory(String id) {
        return translationHistoryRepository.delete(id);
    }
} 