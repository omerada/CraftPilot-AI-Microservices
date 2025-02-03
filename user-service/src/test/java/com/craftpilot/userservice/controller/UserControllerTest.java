package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(UserController.class)
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id("test-id")
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void createUser_Success() {
        when(userService.createUser(anyString())).thenReturn(Mono.just(testUser));

        webTestClient.post()
                .uri("/api/users")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("test-id")
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.role").isEqualTo("USER")
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void getUserById_Success() {
        when(userService.getUserById("test-id")).thenReturn(Mono.just(testUser));

        webTestClient.get()
                .uri("/api/users/test-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("test-id")
                .jsonPath("$.email").isEqualTo("test@example.com");
    }

    @Test
    void updateUser_Success() {
        when(userService.updateUser(anyString(), any(UserEntity.class)))
                .thenReturn(Mono.just(testUser));

        webTestClient.put()
                .uri("/api/users/test-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("test-id");
    }

    @Test
    void deleteUser_Success() {
        when(userService.deleteUser("test-id")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/users/test-id")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void updateUserStatus_Success() {
        when(userService.updateUserStatus("test-id", "INACTIVE"))
                .thenReturn(Mono.just(testUser));

        webTestClient.put()
                .uri("/api/users/test-id/status?status=INACTIVE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("test-id");
    }

    @Test
    void getUserById_NotFound() {
        when(userService.getUserById("non-existent-id"))
                .thenReturn(Mono.error(new UserNotFoundException("User not found")));

        webTestClient.get()
                .uri("/api/users/non-existent-id")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User not found");
    }

    @Test
    void createUser_InvalidToken() {
        when(userService.createUser(anyString()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid token")));

        webTestClient.post()
                .uri("/api/users")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid token");
    }

    @Test
    void updateUserStatus_InvalidStatus() {
        when(userService.updateUserStatus("test-id", "INVALID_STATUS"))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid status")));

        webTestClient.put()
                .uri("/api/users/test-id/status?status=INVALID_STATUS")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid status");
    }
} 