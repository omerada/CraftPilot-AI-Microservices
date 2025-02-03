package com.craftpilot.userservice.integration;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient; 

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private FirebaseToken firebaseToken;

    private UserEntity testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Test verilerini temizle
        userRepository.deleteById("test-id").block();

        // Test kullanıcısını oluştur
        testUser = UserEntity.builder()
                .id("test-id")
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        // Firebase mock
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("test-id");
        when(firebaseToken.getEmail()).thenReturn("test@example.com");
        when(firebaseToken.getName()).thenReturn("Test User");
    }

    @Test
    void createUser_Success() {
        webTestClient.post()
                .uri("/api/users")
                .header("Authorization", "Bearer test-token")
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
        // Önce kullanıcıyı kaydet
        userRepository.save(testUser).block();

        webTestClient.get()
                .uri("/api/users/{id}", testUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(testUser.getId())
                .jsonPath("$.email").isEqualTo(testUser.getEmail());
    }

    @Test
    void updateUser_Success() {
        // Önce kullanıcıyı kaydet
        userRepository.save(testUser).block();

        UserEntity updates = UserEntity.builder()
                .username("newusername")
                .displayName("Updated Name")
                .build();

        webTestClient.put()
                .uri("/api/users/{id}", testUser.getId())
                .bodyValue(updates)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("newusername")
                .jsonPath("$.displayName").isEqualTo("Updated Name");
    }

    @Test
    void deleteUser_Success() {
        // Önce kullanıcıyı kaydet
        userRepository.save(testUser).block();

        webTestClient.delete()
                .uri("/api/users/{id}", testUser.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void updateUserStatus_Success() {
        // Önce kullanıcıyı kaydet
        userRepository.save(testUser).block();

        webTestClient.put()
                .uri("/api/users/{id}/status?status=INACTIVE", testUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE");
    }

    @Test
    void searchUsers_ByEmail_Success() {
        // Önce kullanıcıyı kaydet
        userRepository.save(testUser).block();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/search")
                        .queryParam("email", testUser.getEmail())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(testUser.getId());
    }
} 