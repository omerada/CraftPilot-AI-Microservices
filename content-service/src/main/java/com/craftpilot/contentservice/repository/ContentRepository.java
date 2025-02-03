package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentStatus;
import com.craftpilot.contentservice.model.ContentType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface ContentRepository {
    Mono<Content> save(Content content);
    Mono<Content> findById(String id);
    Flux<Content> findByUserId(String userId);
    Flux<Content> findByType(ContentType type);
    Flux<Content> findByStatus(ContentStatus status);
    Flux<Content> findByTags(List<String> tags);
    Flux<Content> findByMetadata(Map<String, String> metadata);
    Mono<Void> deleteById(String id);
    Flux<Content> findAll();
    Flux<Content> search(String query);
} 