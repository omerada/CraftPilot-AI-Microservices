package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.model.ContentType;
import com.craftpilot.contentservice.testcontainers.FirestoreEmulatorContainer; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Testcontainers
class ContentRepositoryTest {

    @Container
    static FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
    );

    @DynamicPropertySource
    static void firestoreProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gcp.firestore.host-port", emulator::getEmulatorEndpoint);
        registry.add("spring.cloud.gcp.firestore.project-id", () -> "test-project");
    }

    @Autowired
    private ContentRepository contentRepository;

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

    @Test
    void save_Success() {
        StepVerifier.create(contentRepository.save(content))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findById_Success() {
        StepVerifier.create(contentRepository.save(content)
                .then(contentRepository.findById(content.getId())))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findById_NotFound() {
        StepVerifier.create(contentRepository.findById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findByUserId_Success() {
        StepVerifier.create(contentRepository.save(content)
                .flatMapMany(saved -> contentRepository.findByUserId(content.getUserId())))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findByType_Success() {
        StepVerifier.create(contentRepository.save(content)
                .flatMapMany(saved -> contentRepository.findByType(content.getType())))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findByTags_Success() {
        StepVerifier.create(contentRepository.save(content)
                .flatMapMany(saved -> contentRepository.findByTags(content.getTags())))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void findByMetadata_Success() {
        Map<String, String> stringMetadata = new HashMap<>();
        content.getMetadata().forEach((key, value) -> stringMetadata.put(key, value.toString()));
        
        StepVerifier.create(contentRepository.save(content)
                .flatMapMany(saved -> contentRepository.findByMetadata(stringMetadata)))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void deleteById_Success() {
        StepVerifier.create(contentRepository.save(content)
                .then(contentRepository.deleteById(content.getId()))
                .then(contentRepository.findById(content.getId())))
                .verifyComplete();
    }
} 