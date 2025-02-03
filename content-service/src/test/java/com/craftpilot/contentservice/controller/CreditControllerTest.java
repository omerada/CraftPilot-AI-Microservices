package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.Credit;
import com.craftpilot.contentservice.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(CreditController.class)
class CreditControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreditService creditService;

    private Credit credit;
    private final String userId = "test-user";

    @BeforeEach
    void setUp() {
        credit = Credit.builder()
                .id("1")
                .userId(userId)
                .credits(100)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createCredit_Success() {
        when(creditService.createCredit(userId)).thenReturn(Mono.just(credit));

        webTestClient.post()
                .uri("/api/v1/credits/{userId}", userId)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Credit.class)
                .isEqualTo(credit);
    }

    @Test
    void getCredit_Success() {
        when(creditService.getCredit(userId)).thenReturn(Mono.just(credit));

        webTestClient.get()
                .uri("/api/v1/credits/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Credit.class)
                .isEqualTo(credit);
    }

    @Test
    void getCredit_NotFound() {
        when(creditService.getCredit(userId))
                .thenReturn(Mono.error(new RuntimeException("Credit not found for user: " + userId)));

        webTestClient.get()
                .uri("/api/v1/credits/{userId}", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void updateCredits_Success() {
        when(creditService.updateCredits(eq(userId), anyInt())).thenReturn(Mono.just(credit));

        webTestClient.put()
                .uri("/api/v1/credits/{userId}?credits=200", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Credit.class)
                .isEqualTo(credit);
    }

    @Test
    void deductCredits_Success() {
        when(creditService.deductCredits(eq(userId), anyInt())).thenReturn(Mono.just(credit));

        webTestClient.put()
                .uri("/api/v1/credits/{userId}/deduct?credits=50", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Credit.class)
                .isEqualTo(credit);
    }

    @Test
    void deductCredits_InsufficientCredits() {
        when(creditService.deductCredits(eq(userId), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Insufficient credits")));

        webTestClient.put()
                .uri("/api/v1/credits/{userId}/deduct?credits=150", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void addCredits_Success() {
        when(creditService.addCredits(eq(userId), anyInt())).thenReturn(Mono.just(credit));

        webTestClient.put()
                .uri("/api/v1/credits/{userId}/add?credits=50", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Credit.class)
                .isEqualTo(credit);
    }

    @Test
    void deleteCredit_Success() {
        when(creditService.deleteCredit(userId)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/credits/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteCredit_NotFound() {
        when(creditService.deleteCredit(userId))
                .thenReturn(Mono.error(new RuntimeException("Credit not found for user: " + userId)));

        webTestClient.delete()
                .uri("/api/v1/credits/{userId}", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }
} 