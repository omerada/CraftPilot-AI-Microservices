package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.ChatHistory;
import com.craftpilot.contentservice.model.ChatMessage;
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
import java.util.UUID;

@SpringBootTest
@Testcontainers
class ChatHistoryRepositoryTest {

    @Container
    static FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
    );

    @DynamicPropertySource
    static void configureFirestoreProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gcp.firestore.emulator.enabled", () -> "true");
        registry.add("spring.cloud.gcp.firestore.host-port", emulator::getEmulatorEndpoint);
    }

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    private ChatHistory chatHistory;

    @BeforeEach
    void setUp() {
        ChatMessage message = ChatMessage.builder()
                .role("user")
                .content("Test message")
                .timestamp(Instant.now())
                .build();

        chatHistory = ChatHistory.builder()
                .id(UUID.randomUUID().toString())
                .userId("test-user")
                .contentId("test-content")
                .messages(Arrays.asList(message))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_Success() {
        StepVerifier.create(chatHistoryRepository.save(chatHistory))
                .expectNextMatches(saved -> saved.getId().equals(chatHistory.getId()))
                .verifyComplete();
    }

    @Test
    void findById_Success() {
        StepVerifier.create(chatHistoryRepository.save(chatHistory)
                .flatMap(saved -> chatHistoryRepository.findById(saved.getId())))
                .expectNextMatches(found -> found.getId().equals(chatHistory.getId()))
                .verifyComplete();
    }

    @Test
    void findByUserId_Success() {
        StepVerifier.create(chatHistoryRepository.save(chatHistory)
                .flatMapMany(saved -> chatHistoryRepository.findByUserId(saved.getUserId())))
                .expectNextMatches(found -> found.getUserId().equals(chatHistory.getUserId()))
                .verifyComplete();
    }

    @Test
    void findByContentId_Success() {
        StepVerifier.create(chatHistoryRepository.save(chatHistory)
                .flatMapMany(saved -> chatHistoryRepository.findByContentId(saved.getContentId())))
                .expectNextMatches(found -> found.getContentId().equals(chatHistory.getContentId()))
                .verifyComplete();
    }

    @Test
    void deleteById_Success() {
        StepVerifier.create(chatHistoryRepository.save(chatHistory)
                .flatMap(saved -> chatHistoryRepository.deleteById(saved.getId())))
                .verifyComplete();
    }
} 