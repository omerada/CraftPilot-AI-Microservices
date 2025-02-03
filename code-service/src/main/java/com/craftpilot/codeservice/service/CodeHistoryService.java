package com.craftpilot.codeservice.service;

import com.craftpilot.codeservice.model.CodeHistory;
import com.craftpilot.codeservice.repository.CodeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CodeHistoryService {
    private final CodeHistoryRepository codeHistoryRepository;

    public Mono<CodeHistory> saveCodeHistory(CodeHistory codeHistory) {
        return codeHistoryRepository.save(codeHistory);
    }

    public Mono<CodeHistory> getCodeHistory(String id) {
        return codeHistoryRepository.findById(id);
    }

    public Flux<CodeHistory> getUserCodeHistories(String userId) {
        return codeHistoryRepository.findByUserId(userId);
    }

    public Mono<Void> deleteCodeHistory(String id) {
        return codeHistoryRepository.deleteById(id);
    }
} 