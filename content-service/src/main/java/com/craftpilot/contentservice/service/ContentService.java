package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.exception.ResourceNotFoundException;
import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentStatus;
import com.craftpilot.contentservice.model.ContentType;
import com.craftpilot.contentservice.model.dto.ContentRequest;
import com.craftpilot.contentservice.repository.ContentCacheRepository;
import com.craftpilot.contentservice.repository.ContentRepository;
import com.craftpilot.contentservice.service.ai.OpenAIService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {
    private final ContentRepository contentRepository;
    private final ContentCacheRepository cacheRepository;
    private final EventPublisherService eventPublisher;
    private final OpenAIService openAIService; 

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<Content> createContent(String userId, ContentRequest request) {
        Content content = Content.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .status(ContentStatus.DRAFT)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return contentRepository.save(content)
                .doOnSuccess(savedContent -> {
                    log.info("Content created successfully: {}", savedContent.getId());
                    eventPublisher.publishContentCreated(savedContent);
                })
                .doOnError(error -> log.error("Error creating content", error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<Content> getContent(String id) {
        return contentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Content not found with id: " + id)))
                .doOnSuccess(content -> log.info("Content retrieved successfully: {}", id))
                .doOnError(error -> log.error("Error retrieving content: {}", id, error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Flux<Content> getAllContents() {
        return contentRepository.findAll()
                .doOnComplete(() -> log.info("All contents retrieved successfully"))
                .doOnError(error -> log.error("Error retrieving all contents", error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<Content> updateContent(String id, String userId, ContentRequest request) {
        return contentRepository.findById(id)
                .filter(content -> content.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Content not found with id: " + id)))
                .map(existingContent -> {
                    existingContent.setTitle(request.getTitle());
                    existingContent.setDescription(request.getDescription());
                    existingContent.setType(request.getType());
                    existingContent.setTags(request.getTags());
                    existingContent.setMetadata(request.getMetadata());
                    existingContent.setUpdatedBy(userId);
                    existingContent.setUpdatedAt(Instant.now());
                    return existingContent;
                })
                .flatMap(contentRepository::save)
                .doOnSuccess(updatedContent -> {
                    log.info("Content updated successfully: {}", id);
                    eventPublisher.publishContentUpdated(updatedContent);
                })
                .doOnError(error -> log.error("Error updating content: {}", id, error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Mono<Void> deleteContent(String id, String userId) {
        return contentRepository.findById(id)
                .filter(content -> content.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Content not found with id: " + id)))
                .flatMap(content -> contentRepository.deleteById(id)
                        .doOnSuccess(v -> {
                            log.info("Content deleted successfully: {}", id);
                            eventPublisher.publishContentDeleted(id, userId);
                        }))
                .doOnError(error -> log.error("Error deleting content: {}", id, error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Flux<Content> getContentsByType(String type) {
        return contentRepository.findByType(ContentType.valueOf(type))
                .doOnComplete(() -> log.info("Contents retrieved successfully by type: {}", type))
                .doOnError(error -> log.error("Error retrieving contents by type: {}", type, error));
    }

    @CircuitBreaker(name = "contentService")
    @RateLimiter(name = "contentService")
    public Flux<Content> searchContents(String query) {
        return contentRepository.search(query)
                .doOnComplete(() -> log.info("Contents searched successfully with query: {}", query))
                .doOnError(error -> log.error("Error searching contents with query: {}", query, error));
    }

    @CircuitBreaker(name = "default")
    @RateLimiter(name = "default")
    public Mono<Content> generateContent(String userId, ContentRequest request) {
        Content content = Content.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .status(ContentStatus.DRAFT)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return openAIService.generateContent(content)
                .map(generatedContent -> {
                    content.setContent(generatedContent);
                    return content;
                })
                .flatMap(contentRepository::save)
                .flatMap(cacheRepository::save)
                .doOnSuccess(saved -> eventPublisher.publishContentCreated(saved))
                .doOnError(error -> log.error("Error generating content: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    @RateLimiter(name = "default")
    public Mono<Content> improveContent(String id, String userId) {
        return contentRepository.findById(id)
                .filter(content -> content.getUserId().equals(userId))
                .flatMap(content -> openAIService.improveContent(content)
                        .map(improvedContent -> {
                            content.setContent(improvedContent);
                            content.setUpdatedBy(userId);
                            content.setUpdatedAt(Instant.now());
                            content.setVersion(content.getVersion() + 1);
                            return content;
                        }))
                .flatMap(contentRepository::save)
                .flatMap(cacheRepository::save)
                .doOnSuccess(improved -> eventPublisher.publishContentUpdated(improved))
                .doOnError(error -> log.error("Error improving content: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Mono<Content> publishContent(String id, String userId) {
        return contentRepository.findById(id)
                .filter(content -> content.getUserId().equals(userId))
                .map(content -> {
                    content.setStatus(ContentStatus.PUBLISHED);
                    content.setUpdatedBy(userId);
                    content.setUpdatedAt(Instant.now());
                    return content;
                })
                .flatMap(contentRepository::save)
                .flatMap(cacheRepository::save)
                .doOnSuccess(published -> eventPublisher.publishContentPublished(published))
                .doOnError(error -> log.error("Error publishing content: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Mono<Content> archiveContent(String id, String userId) {
        return contentRepository.findById(id)
                .filter(content -> content.getUserId().equals(userId))
                .map(content -> {
                    content.setStatus(ContentStatus.ARCHIVED);
                    content.setUpdatedBy(userId);
                    content.setUpdatedAt(Instant.now());
                    return content;
                })
                .flatMap(contentRepository::save)
                .flatMap(cacheRepository::save)
                .doOnSuccess(archived -> eventPublisher.publishContentArchived(archived))
                .doOnError(error -> log.error("Error archiving content: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Flux<Content> getUserContents(String userId) {
        return contentRepository.findByUserId(userId)
                .doOnError(error -> log.error("Error retrieving user contents: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Flux<Content> getContentsByStatus(ContentStatus status) {
        return contentRepository.findByStatus(status)
                .doOnError(error -> log.error("Error retrieving contents by status: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Flux<Content> getContentsByTags(List<String> tags) {
        return contentRepository.findByTags(tags)
                .doOnError(error -> log.error("Error retrieving contents by tags: {}", error.getMessage()));
    }

    @CircuitBreaker(name = "default")
    public Flux<Content> getContentsByMetadata(Map<String, String> metadata) {
        return contentRepository.findByMetadata(metadata)
                .doOnError(error -> log.error("Error retrieving contents by metadata: {}", error.getMessage()));
    }
} 