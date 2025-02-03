package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Credit;
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

@SpringBootTest
@Testcontainers
class CreditRepositoryTest {

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
    private CreditRepository creditRepository;

    private Credit credit;

    @BeforeEach
    void setUp() {
        credit = Credit.builder()
                .id("1")
                .userId("user1")
                .credits(100)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_Success() {
        StepVerifier.create(creditRepository.save(credit))
                .expectNext(credit)
                .verifyComplete();
    }

    @Test
    void findById_Success() {
        StepVerifier.create(creditRepository.save(credit)
                .then(creditRepository.findById(credit.getId())))
                .expectNext(credit)
                .verifyComplete();
    }

    @Test
    void findById_NotFound() {
        StepVerifier.create(creditRepository.findById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findByUserId_Success() {
        StepVerifier.create(creditRepository.save(credit)
                .then(creditRepository.findByUserId(credit.getUserId())))
                .expectNext(credit)
                .verifyComplete();
    }

    @Test
    void updateCredits_Success() {
        StepVerifier.create(creditRepository.save(credit)
                .then(creditRepository.updateCredits(credit.getUserId(), 200))
                .flatMap(updated -> creditRepository.findById(credit.getId())))
                .expectNextMatches(updated -> updated.getCredits() == 200)
                .verifyComplete();
    }

    @Test
    void deleteById_Success() {
        StepVerifier.create(creditRepository.save(credit)
                .then(creditRepository.deleteById(credit.getId()))
                .then(creditRepository.findById(credit.getId())))
                .verifyComplete();
    }

    @Test
    void deleteByUserId_Success() {
        StepVerifier.create(creditRepository.save(credit)
                .then(creditRepository.deleteByUserId(credit.getUserId()))
                .then(creditRepository.findByUserId(credit.getUserId())))
                .verifyComplete();
    }
} 