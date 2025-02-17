package com.craftpilot.userservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus; 
import com.craftpilot.userservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.craftpilot.userservice.model.user.event.UserEvent;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;

/**
 * Kullanıcı işlemleri için servis arayüzü.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final Validator validator;
    private final MeterRegistry meterRegistry;
    private final RedisCacheService cacheService;
    private final KafkaService kafkaService;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;

    private Counter userCreationCounter;
    private Counter userUpdateCounter;
    private Counter userDeletionCounter;
    private Timer userRetrievalTimer;

    @PostConstruct
    public void init() {
        this.userCreationCounter = meterRegistry.counter("user.creation", "type", "create");
        this.userUpdateCounter = meterRegistry.counter("user.update", "type", "update");
        this.userDeletionCounter = meterRegistry.counter("user.deletion", "type", "delete");
        this.userRetrievalTimer = meterRegistry.timer("user.retrieval", "type", "get");
    }

    @Transactional
    public Mono<UserEntity> createUser(UserEntity user) {
        return userRepository.save(user)
                .doOnSuccess(savedUser -> {
                    kafkaService.sendUserCreatedEvent(savedUser);
                    sendUserEvent(savedUser, "USER_CREATED");
                })
                .doOnError(error -> log.error("Error creating user: {}", error.getMessage()));
    }

    public Mono<UserEntity> getUserById(String id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .doFinally(signalType -> sample.stop(userRetrievalTimer));
    }

    @Transactional
    public Mono<UserEntity> updateUser(String userId, UserEntity updates) {
        return userRepository.findById(userId)
                .flatMap(existingUser -> {
                    if (updates.getUsername() != null) {
                        existingUser.setUsername(updates.getUsername());
                    }
                    if (updates.getDisplayName() != null) {
                        existingUser.setDisplayName(updates.getDisplayName());
                    }
                    if (updates.getPhotoUrl() != null) {
                        existingUser.setPhotoUrl(updates.getPhotoUrl());
                    }
                    if (updates.getStatus() != null) {
                        existingUser.setStatus(updates.getStatus());
                    }
                    existingUser.setUpdatedAt(System.currentTimeMillis());
                    return userRepository.save(existingUser);
                })
                .doOnSuccess(updatedUser -> {
                    kafkaService.sendUserUpdatedEvent(updatedUser);
                    sendUserEvent(updatedUser, "USER_UPDATED");
                })
                .doOnError(error -> log.error("Error updating user: {}", error.getMessage()));
    }

    public Mono<Void> deleteUser(String userId) {
        return userRepository.findById(userId)
                .flatMap(user -> userRepository.deleteById(userId)
                        .then(Mono.fromRunnable(() -> {
                            kafkaService.sendUserDeletedEvent(user);
                            sendUserEvent(user, "USER_DELETED");
                        })))
                .then();
    }

    public Mono<UserEntity> updateUserStatus(String userId, UserStatus status) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setStatus(status);
                    user.setUpdatedAt(System.currentTimeMillis());
                    return userRepository.save(user);
                })
                .doOnSuccess(updatedUser -> kafkaService.sendUserUpdatedEvent(updatedUser))
                .doOnError(error -> log.error("Error updating user status: {}", error.getMessage()));
    }

    public Mono<UserEntity> searchUsers(String email, String username) {
        if (email != null && !email.isEmpty()) {
            return userRepository.findByEmail(email);
        } else if (username != null && !username.isEmpty()) {
            return userRepository.findByUsername(username);
        }
        return Mono.error(new IllegalArgumentException("Either email or username must be provided"));
    }

    public Mono<FirebaseToken> verifyFirebaseToken(String token) {
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
                .doOnError(e -> log.error("Error verifying Firebase token: {}", e.getMessage()));
    }

    private UserEntity buildUserFromToken(FirebaseToken token) {
        return UserEntity.builder()
                .id(token.getUid())
                .email(token.getEmail())
                .username(generateUsername(token.getEmail()))
                .displayName(token.getName())
                .photoUrl(token.getPicture())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }

    private void updateUserFields(UserEntity existingUser, UserEntity updates) {
        if (updates.getUsername() != null) {
            existingUser.setUsername(updates.getUsername());
        }
        if (updates.getDisplayName() != null) {
            existingUser.setDisplayName(updates.getDisplayName());
        }
        if (updates.getPhotoUrl() != null) {
            existingUser.setPhotoUrl(updates.getPhotoUrl());
        }
        if (updates.getStatus() != null) {
            existingUser.setStatus(updates.getStatus());
        }
        existingUser.setUpdatedAt(System.currentTimeMillis());
    }

    private String generateUsername(String email) {
        return email.split("@")[0] + "_" + System.currentTimeMillis() % 1000;
    }

    private void validateUserUpdate(UserEntity updates) {
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(updates);
        if (!violations.isEmpty()) {
            throw new ValidationException(
                violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "))
            );
        }
    }

    public Mono<UserEntity> verifyAndCreateUser(String firebaseToken) {
        return verifyFirebaseToken(firebaseToken)
                .map(decodedToken -> UserEntity.builder()
                        .id(decodedToken.getUid())
                        .email(decodedToken.getEmail())
                        .displayName(decodedToken.getName())
                        .photoUrl(decodedToken.getPicture())
                        .status(UserStatus.ACTIVE)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build())
                .flatMap(this::createUser);
    }

    public Mono<UserEntity> verifyAndCreateOrUpdateUser(String firebaseToken) {
        return verifyFirebaseToken(firebaseToken)
            .flatMap(decodedToken -> userRepository.findById(decodedToken.getUid())
                .flatMap(existingUser -> {
                    // Update existing user if needed
                    boolean needsUpdate = false;
                    if (!existingUser.getEmail().equals(decodedToken.getEmail())) {
                        existingUser.setEmail(decodedToken.getEmail());
                        needsUpdate = true;
                    }
                    if (!existingUser.getDisplayName().equals(decodedToken.getName())) {
                        existingUser.setDisplayName(decodedToken.getName());
                        needsUpdate = true;
                    }
                    if (needsUpdate) {
                        existingUser.setUpdatedAt(System.currentTimeMillis());
                        return userRepository.save(existingUser);
                    }
                    return Mono.just(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new user if not exists
                    UserEntity newUser = buildUserFromToken(decodedToken);
                    return createUser(newUser);
                })));
    }

    public Mono<UserEntity> handleFirebaseUpdate(String userId, UserEntity updates) {
        return userRepository.findById(userId)
            .flatMap(existingUser -> {
                updateUserFields(existingUser, updates);
                return userRepository.save(existingUser)
                    .doOnSuccess(savedUser -> {
                        // Firebase ile senkronize et
                        try {
                            UpdateRequest request = new UpdateRequest(userId)
                                .setDisplayName(updates.getDisplayName())
                                .setPhotoUrl(updates.getPhotoUrl());
                            firebaseAuth.updateUser(request);
                        } catch (FirebaseAuthException e) {
                            log.error("Firebase update failed", e);
                        }
                    });
            });
    }

    private void sendUserEvent(UserEntity user, String eventType) {
        UserEvent event = UserEvent.fromEntity(user, eventType);
        
        kafkaTemplate.send(userEventsTopic, user.getId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send user event: {}", ex.getMessage());
                } else {
                    log.debug("User event sent successfully: {}", eventType);
                }
            });
    }

    public Mono<UserEntity> findById(String id) {
        return cacheService.get(id)
                .switchIfEmpty(userRepository.findById(id)
                        .flatMap(user -> cacheService.set(user.getId(), user)
                                .thenReturn(user)));
    }
}
