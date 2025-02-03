package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

@SpringBootTest
@Testcontainers
class ContentCacheRepositoryTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ContentCacheRepository contentCacheRepository;

    private Content content;

    @BeforeEach
    void setUp() {
        content = Content.builder()
                .id("1")
                .title("Test Title")
                .description("Test Description")
                .content("Test Content")
                .type(ContentType.ARTICLE)
                .tags(Arrays.asList("test", "sample"))
                .metadata(new HashMap<>())
                .userId("user1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (redis != null && redis.isRunning()) {
            redis.close();
        }
    }

    @Test
    void save_Success() {
        StepVerifier.create(contentCacheRepository.save(content))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findById_Success() {
        StepVerifier.create(contentCacheRepository.save(content)
                .then(contentCacheRepository.findById(content.getId())))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findById_NotFound() {
        StepVerifier.create(contentCacheRepository.findById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void deleteById_Success() {
        StepVerifier.create(contentCacheRepository.save(content)
                .then(contentCacheRepository.deleteById(content.getId()))
                .then(contentCacheRepository.findById(content.getId())))
                .verifyComplete();
    }
} 