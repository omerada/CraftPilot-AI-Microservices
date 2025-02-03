package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceCacheTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RedisCacheService cacheService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private KafkaService kafkaService;

    @Test
    void getUserById_ShouldUseCacheOnSecondCall() {
        UserEntity testUser = UserEntity.builder()
                .id("test-id")
                .email("test@example.com")
                .build();

        when(cacheService.getCachedUser("test-id"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(testUser));

        when(userRepository.findById("test-id"))
                .thenReturn(Mono.just(testUser));

        when(cacheService.cacheUser(testUser))
                .thenReturn(Mono.just(testUser));

        // İlk çağrı - cache miss, repository'den gelecek
        StepVerifier.create(userService.getUserById("test-id"))
                .expectNext(testUser)
                .verifyComplete();

        // İkinci çağrı - cache hit
        StepVerifier.create(userService.getUserById("test-id"))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepository, times(1)).findById("test-id");
        verify(cacheService, times(2)).getCachedUser("test-id");
        verify(cacheService, times(1)).cacheUser(testUser);
    }

    @Test
    void deleteUser_ShouldEvictCache() {
        UserEntity testUser = UserEntity.builder()
                .id("test-id")
                .email("test@example.com")
                .build();

        when(userRepository.deleteById("test-id"))
                .thenReturn(Mono.empty());

        when(cacheService.invalidateUser("test-id"))
                .thenReturn(Mono.just(true));

        when(kafkaService.sendUserDeletedEvent("test-id"))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteUser("test-id"))
                .verifyComplete();

        verify(userRepository).deleteById("test-id");
        verify(cacheService).invalidateUser("test-id");
        verify(kafkaService).sendUserDeletedEvent("test-id");
    }
} 