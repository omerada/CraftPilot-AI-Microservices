package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentStatus;
import com.craftpilot.contentservice.model.ContentType;
import com.craftpilot.contentservice.model.dto.ContentRequest;
import com.craftpilot.contentservice.repository.ContentCacheRepository;
import com.craftpilot.contentservice.repository.ContentRepository;
import com.craftpilot.contentservice.service.ai.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List; 

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentCacheRepository cacheRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @Mock
    private OpenAIService openAIService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private ContentService contentService;

    private Content testContent;
    private ContentRequest testRequest;
    private final String userId = "test-user";
    private final String contentId = "test-content-id";
    private final String generatedContent = "Generated content";

    @BeforeEach
    void setUp() {
        testContent = Content.builder()
                .id(contentId)
                .userId(userId)
                .title("Test Content")
                .description("Test Description")
                .type(ContentType.TEXT)
                .content("Test Content Body")
                .tags(Arrays.asList("test", "sample"))
                .metadata(Collections.singletonMap("key", "value"))
                .status(ContentStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(userId)
                .updatedBy(userId)
                .version(1)
                .build();

        testRequest = ContentRequest.builder()
                .title("Test Content")
                .description("Test Description")
                .type(ContentType.TEXT)
                .content("Test Content Body")
                .tags(Arrays.asList("test", "sample"))
                .metadata(Collections.singletonMap("key", "value"))
                .build();
    }

    @Test
    void generateContent_Success() {
        when(openAIService.generateContent(any(Content.class))).thenReturn(Mono.just(generatedContent));
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        doNothing().when(eventPublisher).publishContentCreated(any(Content.class));

        StepVerifier.create(contentService.generateContent(userId, testRequest))
                .expectNext(testContent)
                .verifyComplete();

        verify(openAIService).generateContent(any(Content.class));
        verify(contentRepository).save(any(Content.class));
        verify(cacheRepository).save(any(Content.class));
        verify(eventPublisher).publishContentCreated(any(Content.class));
    }

    @Test
    void improveContent_Success() {
        when(contentRepository.findById(contentId)).thenReturn(Mono.just(testContent));
        when(openAIService.improveContent(testContent)).thenReturn(Mono.just(generatedContent));
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        doNothing().when(eventPublisher).publishContentUpdated(any(Content.class));

        StepVerifier.create(contentService.improveContent(contentId, userId))
                .expectNext(testContent)
                .verifyComplete();

        verify(contentRepository).findById(contentId);
        verify(openAIService).improveContent(testContent);
        verify(contentRepository).save(any(Content.class));
        verify(cacheRepository).save(any(Content.class));
        verify(eventPublisher).publishContentUpdated(any(Content.class));
    }

    @Test
    void createContent_Success() {
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        doNothing().when(eventPublisher).publishContentCreated(any(Content.class));

        StepVerifier.create(contentService.createContent(userId, testRequest))
                .expectNext(testContent)
                .verifyComplete();

        verify(contentRepository).save(any(Content.class));
        verify(cacheRepository).save(any(Content.class));
        verify(eventPublisher).publishContentCreated(any(Content.class));
    }

    @Test
    void getContent_Success() {
        when(cacheRepository.findById(contentId)).thenReturn(Mono.empty());
        when(contentRepository.findById(contentId)).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(testContent)).thenReturn(Mono.just(testContent));

        StepVerifier.create(contentService.getContent(contentId))
                .expectNext(testContent)
                .verifyComplete();

        verify(cacheRepository).findById(contentId);
        verify(contentRepository).findById(contentId);
        verify(cacheRepository).save(testContent);
    }

    @Test
    void updateContent_Success() {
        when(contentRepository.findById(contentId)).thenReturn(Mono.just(testContent));
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        doNothing().when(eventPublisher).publishContentUpdated(any(Content.class));

        StepVerifier.create(contentService.updateContent(contentId, userId, testRequest))
                .expectNext(testContent)
                .verifyComplete();

        verify(contentRepository).findById(contentId);
        verify(contentRepository).save(any(Content.class));
        verify(cacheRepository).save(any(Content.class));
        verify(eventPublisher).publishContentUpdated(any(Content.class));
    }

    @Test
    void deleteContent_Success() {
        when(contentRepository.findById(contentId)).thenReturn(Mono.just(testContent));
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(testContent));
        when(cacheRepository.deleteById(contentId)).thenReturn(Mono.empty());
        doNothing().when(eventPublisher).publishContentDeleted(contentId, userId);

        StepVerifier.create(contentService.deleteContent(contentId, userId))
                .verifyComplete();

        verify(contentRepository).findById(contentId);
        verify(contentRepository).save(any(Content.class));
        verify(cacheRepository).deleteById(contentId);
        verify(eventPublisher).publishContentDeleted(contentId, userId);
    }

    @Test
    void getUserContents_Success() {
        List<Content> contentList = Arrays.asList(testContent);
        when(contentRepository.findByUserId(userId)).thenReturn(Flux.fromIterable(contentList));

        StepVerifier.create(contentService.getUserContents(userId))
                .expectNextSequence(contentList)
                .verifyComplete();

        verify(contentRepository).findByUserId(userId);
    }

    @Test
    void getContentsByType_Success() {
        List<Content> contentList = Arrays.asList(testContent);
        when(contentRepository.findByType(ContentType.TEXT)).thenReturn(Flux.fromIterable(contentList));

        StepVerifier.create(contentService.getContentsByType(ContentType.TEXT.name()))
                .expectNextSequence(contentList)
                .verifyComplete();

        verify(contentRepository).findByType(ContentType.TEXT);
    }

    @Test
    void getContentsByStatus_Success() {
        List<Content> contentList = Arrays.asList(testContent);
        when(contentRepository.findByStatus(ContentStatus.DRAFT)).thenReturn(Flux.fromIterable(contentList));

        StepVerifier.create(contentService.getContentsByStatus(ContentStatus.DRAFT))
                .expectNextSequence(contentList)
                .verifyComplete();

        verify(contentRepository).findByStatus(ContentStatus.DRAFT);
    }

    @Test
    void getContentsByTags_Success() {
        List<Content> contentList = Arrays.asList(testContent);
        List<String> tags = Arrays.asList("test", "sample");
        when(contentRepository.findByTags(tags)).thenReturn(Flux.fromIterable(contentList));

        StepVerifier.create(contentService.getContentsByTags(tags))
                .expectNextSequence(contentList)
                .verifyComplete();

        verify(contentRepository).findByTags(tags);
    }

    @Test
    void getContent_CacheMiss_Success() {
        when(cacheRepository.findById(contentId)).thenReturn(Mono.empty());
        when(contentRepository.findById(contentId)).thenReturn(Mono.just(testContent));
        when(cacheRepository.save(testContent)).thenReturn(Mono.just(testContent));

        StepVerifier.create(contentService.getContent(contentId))
                .expectNext(testContent)
                .verifyComplete();
    }

    @Test
    void getContent_NotFound() {
        when(cacheRepository.findById(contentId)).thenReturn(Mono.empty());
        when(contentRepository.findById(contentId)).thenReturn(Mono.empty());

        StepVerifier.create(contentService.getContent(contentId))
                .verifyComplete();
    }

    @Test
    void getAllContents_Success() {
        when(contentRepository.findAll()).thenReturn(Flux.just(testContent));

        StepVerifier.create(contentService.getAllContents())
                .expectNext(testContent)
                .verifyComplete();
    }

    @Test
    void updateContent_NotFound() {
        when(contentRepository.findById(contentId)).thenReturn(Mono.empty());

        StepVerifier.create(contentService.updateContent(contentId, userId, testRequest))
                .verifyComplete();
    }

    @Test
    void deleteContent_NotFound() {
        when(contentRepository.findById(contentId)).thenReturn(Mono.empty());

        StepVerifier.create(contentService.deleteContent(contentId, userId))
                .verifyComplete();
    }
} 