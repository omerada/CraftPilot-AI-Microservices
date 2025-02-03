package com.craftpilot.imageservice.service;

import com.craftpilot.imageservice.model.ImageHistory;
import com.craftpilot.imageservice.repository.ImageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImageHistoryService {
    private final ImageHistoryRepository imageHistoryRepository;

    public Mono<ImageHistory> saveImageHistory(ImageHistory imageHistory) {
        return imageHistoryRepository.save(imageHistory);
    }

    public Mono<ImageHistory> getImageHistory(String id) {
        return imageHistoryRepository.findById(id);
    }

    public Flux<ImageHistory> getUserImageHistories(String userId) {
        return imageHistoryRepository.findByUserId(userId);
    }

    public Mono<Void> deleteImageHistory(String id) {
        return imageHistoryRepository.deleteById(id);
    }
} 