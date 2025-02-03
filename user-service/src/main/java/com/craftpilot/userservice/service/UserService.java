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
    public Mono<UserEntity> createUser(String idToken) {
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(idToken))
                .onErrorMap(FirebaseAuthException.class, e -> new IllegalArgumentException("Invalid token"))
                .map(this::buildUserFromToken)
                .flatMap(userRepository::save)
                .doOnSuccess(user -> {
                    userCreationCounter.increment();
                    kafkaService.sendUserCreatedEvent(user).subscribe();
                })
                .flatMap(cacheService::cacheUser);
    }

    public Mono<UserEntity> getUserById(String id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return cacheService.getCachedUser(id)
                .switchIfEmpty(userRepository.findById(id)
                        .flatMap(cacheService::cacheUser))
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .doFinally(signalType -> sample.stop(userRetrievalTimer));
    }

    @Transactional
    public Mono<UserEntity> updateUser(String id, UserEntity updates) {
        validateUserUpdate(updates);
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + id)))
                .flatMap(existingUser -> {
                    updateUserFields(existingUser, updates);
                    return userRepository.save(existingUser)
                            .flatMap(cacheService::cacheUser)
                            .doOnSuccess(savedUser -> {
                                userUpdateCounter.increment();
                                kafkaService.sendUserUpdatedEvent(savedUser).subscribe();
                            });
                });
    }

    public Mono<Void> deleteUser(String id) {
        return userRepository.deleteById(id)
                .then(cacheService.invalidateUser(id))
                .then(Mono.fromRunnable(() -> {
                    userDeletionCounter.increment();
                    kafkaService.sendUserDeletedEvent(id).subscribe();
                }));
    }

    public Mono<UserEntity> updateUserStatus(String id, String status) {
        try {
            UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
            return userRepository.findById(id)
                    .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                    .map(user -> {
                        user.setStatus(newStatus);
                        return user;
                    })
                    .flatMap(userRepository::save)
                    .doOnSuccess(user -> kafkaService.sendUserStatusChangedEvent(user).subscribe())
                    .flatMap(cacheService::cacheUser);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid status: " + status));
        }
    }

    public Mono<UserEntity> searchUsers(String email, String username) {
        if (email != null && !email.isEmpty()) {
            return findByEmail(email);
        } else if (username != null && !username.isEmpty()) {
            return findByUsername(username);
        }
        return Mono.error(new IllegalArgumentException("Either email or username must be provided"));
    }

    private Mono<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with email: " + email)))
                .flatMap(cacheService::cacheUser);
    }

    private Mono<FirebaseToken> verifyFirebaseToken(String token) {
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
                .subscribeOn(Schedulers.boundedElastic());
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

    private Mono<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with username: " + username)))
                .flatMap(cacheService::cacheUser);
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
}
