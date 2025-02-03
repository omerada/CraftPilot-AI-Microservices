package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Content;
import reactor.core.publisher.Mono;

public interface ContentCacheRepository {
    Mono<Content> save(Content content);
    Mono<Content> findById(String id);
    Mono<Void> deleteById(String id);
    Mono<Void> deleteAll();
} 