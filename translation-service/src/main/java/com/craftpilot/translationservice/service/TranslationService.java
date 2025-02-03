package com.craftpilot.translationservice.service;

import com.craftpilot.translationservice.model.Translation;
import com.craftpilot.translationservice.model.TranslationHistory;
import com.craftpilot.translationservice.repository.TranslationHistoryRepository;
import com.craftpilot.translationservice.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TranslationService {
    private final TranslationRepository translationRepository;
    private final TranslationHistoryRepository translationHistoryRepository;

    public Mono<Translation> createTranslation(Translation translation) {
        return translationRepository.save(translation);
    }

    public Mono<Translation> getTranslation(String id) {
        return translationRepository.findById(id);
    }

    public Flux<Translation> getTranslationsByUserId(String userId) {
        return translationRepository.findByUserId(userId);
    }

    public Mono<Void> deleteTranslation(String id) {
        return translationRepository.delete(id);
    }

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