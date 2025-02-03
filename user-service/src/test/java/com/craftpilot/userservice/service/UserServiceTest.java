package com.craftpilot.userservice.service;

import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private FirebaseToken firebaseToken;

    @Mock
    private Validator validator;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private RedisCacheService cacheService;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        when(meterRegistry.counter(eq("user.creation"), any(String.class), any(String.class))).thenReturn(counter);
        when(meterRegistry.counter(eq("user.update"), any(String.class), any(String.class))).thenReturn(counter);
        when(meterRegistry.counter(eq("user.deletion"), any(String.class), any(String.class))).thenReturn(counter);
        when(meterRegistry.timer(eq("user.retrieval"), any(String.class), any(String.class))).thenReturn(timer);
        
        when(meterRegistry.config()).thenReturn(mock(MeterRegistry.Config.class));
        when(meterRegistry.config().clock()).thenReturn(mock(io.micrometer.core.instrument.Clock.class));
        
        doNothing().when(counter).increment();
        
        userService = new UserService(
            userRepository,
            firebaseAuth,
            validator,
            meterRegistry,
            cacheService,
            kafkaService
        );
        
        userService.init();

        testUser = UserEntity.builder()
                .id("test-id")
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn(testUser.getId());
        when(firebaseToken.getEmail()).thenReturn(testUser.getEmail());
        when(firebaseToken.getName()).thenReturn(testUser.getDisplayName());
        when(firebaseToken.getPicture()).thenReturn("https://example.com/photo.jpg");

        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Mono.just(testUser));
        when(userRepository.deleteById(testUser.getId())).thenReturn(Mono.empty());
        when(cacheService.cacheUser(any(UserEntity.class))).thenReturn(Mono.just(testUser));
        when(cacheService.getCachedUser(anyString())).thenReturn(Mono.empty());
        when(cacheService.invalidateUser(anyString())).thenReturn(Mono.just(true));
        when(kafkaService.sendUserCreatedEvent(any(UserEntity.class))).thenReturn(Mono.empty());
        when(kafkaService.sendUserUpdatedEvent(any(UserEntity.class))).thenReturn(Mono.empty());
        when(kafkaService.sendUserDeletedEvent(anyString())).thenReturn(Mono.empty());
        when(kafkaService.sendUserStatusChangedEvent(any(UserEntity.class))).thenReturn(Mono.empty());
    }

    @Test
    void createUser_Success() throws Exception {
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("test-id");
        when(firebaseToken.getEmail()).thenReturn("test@example.com");
        when(firebaseToken.getName()).thenReturn("Test User");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.createUser("test-token"))
                .expectNextMatches(user -> 
                    user.getId().equals("test-id") &&
                    user.getEmail().equals("test@example.com") &&
                    user.getRole() == UserRole.USER &&
                    user.getStatus() == UserStatus.ACTIVE
                )
                .verifyComplete();
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById("test-id")).thenReturn(Mono.just(testUser));
        when(cacheService.getCachedUser("test-id")).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserById("test-id"))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void updateUser_Success() {
        UserEntity updates = UserEntity.builder()
                .username("newusername")
                .displayName("New Name")
                .build();

        when(userRepository.findById("test-id")).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.updateUser("test-id", updates))
                .expectNextMatches(user -> 
                    user.getUsername().equals("newusername") &&
                    user.getDisplayName().equals("New Name")
                )
                .verifyComplete();
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.deleteById("test-id")).thenReturn(Mono.empty());
        when(cacheService.invalidateUser("test-id")).thenReturn(Mono.just(true));

        StepVerifier.create(userService.deleteUser("test-id"))
                .verifyComplete();
    }

    @Test
    void updateUserStatus_Success() {
        when(userRepository.findById("test-id")).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.updateUserStatus("test-id", "INACTIVE"))
                .expectNextMatches(user -> user.getStatus() == UserStatus.INACTIVE)
                .verifyComplete();
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById("non-existent-id"))
                .thenReturn(Mono.empty());
        when(cacheService.getCachedUser("non-existent-id"))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserById("non-existent-id"))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void updateUser_NotFound() {
        UserEntity updates = UserEntity.builder()
                .username("newusername")
                .build();

        when(userRepository.findById("non-existent-id"))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.updateUser("non-existent-id", updates))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void updateUserStatus_InvalidStatus() {
        when(userRepository.findById("test-id"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.updateUserStatus("test-id", "INVALID_STATUS"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void createUser_InvalidToken() throws Exception {
        when(firebaseAuth.verifyIdToken(anyString()))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        StepVerifier.create(userService.createUser("invalid-token"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
} 