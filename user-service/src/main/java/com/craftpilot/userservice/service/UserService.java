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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.craftpilot.userservice.model.UserPreference;

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
    private final UserPreferenceService userPreferenceService;

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
        userCreationCounter.increment();
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

    /**
     * Kullanıcı adının başka bir kullanıcı tarafından kullanılıp kullanılmadığını kontrol eder.
     * 
     * @param username Kontrol edilecek kullanıcı adı
     * @return Kullanıcı adı alınmışsa true, değilse false döner
     */
    public Mono<Boolean> isUsernameTaken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Mono.just(false);
        }
        return userRepository.findByUsername(username)
                .map(user -> true)
                .defaultIfEmpty(false);
    }

    /**
     * Verilen temel kullanıcı adından benzersiz bir kullanıcı adı üretir.
     * 
     * @param baseUsername Temel kullanıcı adı
     * @return Benzersiz kullanıcı adı
     */
    public Mono<String> generateUniqueUsername(String baseUsername) {
        if (baseUsername == null || baseUsername.trim().isEmpty()) {
            baseUsername = "user_" + System.currentTimeMillis() % 10000;
        } else {
            // Kullanıcı adını düzelt (sadece alfanumerik karakterler ve alt çizgiler)
            baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
            if (baseUsername.isEmpty()) {
                baseUsername = "user_" + System.currentTimeMillis() % 10000;
            }
        }
        
        final String username = baseUsername;
        
        return isUsernameTaken(username)
                .flatMap(taken -> {
                    if (!taken) {
                        return Mono.just(username);
                    } else {
                        // Kullanıcı adı alınmışsa, sonuna rastgele bir sayı ekle
                        String newUsername = username + "_" + (System.currentTimeMillis() % 10000);
                        return generateUniqueUsername(newUsername);
                    }
                });
    }

    /**
     * Firebase token'ından kullanıcı bilgilerini alır ve benzersiz bir kullanıcı adı ile
     * UserEntity nesnesi oluşturur.
     * 
     * @param token Firebase kimlik token'ı
     * @return Benzersiz kullanıcı adına sahip UserEntity
     */
    private Mono<UserEntity> buildUserWithUniqueUsername(FirebaseToken token) {
        String baseUsername;
        String email = token.getEmail();
        String displayName = token.getName();
        
        // Kullanıcı adını displayName veya email'den oluştur
        if (displayName != null && !displayName.trim().isEmpty()) {
            baseUsername = displayName.toLowerCase().replace(" ", "_");
        } else {
            baseUsername = email != null ? email.split("@")[0].toLowerCase() : null;
        }
        
        return generateUniqueUsername(baseUsername)
                .map(uniqueUsername -> UserEntity.builder()
                        .id(token.getUid())
                        .email(token.getEmail())
                        .username(uniqueUsername)
                        .displayName(token.getName())
                        .photoUrl(token.getPicture())
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build());
    }

    // Kullanıcı oluşturma metodunu güncelleme
    public Mono<UserEntity> verifyAndCreateUser(String firebaseToken) {
        return verifyFirebaseToken(firebaseToken)
                .flatMap(this::buildUserWithUniqueUsername)
                .flatMap(this::createUser);
    }

    // Kullanıcı güncelleme metodunu güncelleme (benzersiz userName kontrolü)
    @Transactional
    public Mono<UserEntity> updateUser(String userId, UserEntity updates) {
        if (updates.getUsername() != null) {
            // Kullanıcı adı değiştirilmek isteniyorsa, benzersizliği kontrol et
            return userRepository.findByUsername(updates.getUsername())
                    .flatMap(existingUser -> {
                        // Eğer bulunan kullanıcı kendisi değilse, bu kullanıcı adı alınmış demektir
                        if (!existingUser.getId().equals(userId)) {
                            return Mono.error(new RuntimeException("Bu kullanıcı adı başkası tarafından kullanılıyor"));
                        }
                        // Kendisiyse güncellemeye devam et
                        return updateUserInternal(userId, updates);
                    })
                    .switchIfEmpty(updateUserInternal(userId, updates)); // Kullanıcı adı alınmamışsa güncellemeye devam et
        }
        // Kullanıcı adı değiştirilmiyorsa normal güncelleme
        return updateUserInternal(userId, updates);
    }
    
    // Kullanıcı güncelleme iç metodu
    private Mono<UserEntity> updateUserInternal(String userId, UserEntity updates) {
        return userRepository.findById(userId)
                .flatMap(existingUser -> {
                    updateUserFields(existingUser, updates);
                    return userRepository.save(existingUser);
                })
                .doOnSuccess(updatedUser -> {
                    kafkaService.sendUserUpdatedEvent(updatedUser);
                    sendUserEvent(updatedUser, "USER_UPDATED");
                })
                .doOnError(error -> log.error("Error updating user: {}", error.getMessage()));
    }

    // Kullanıcı silme metodunu güncelleme (kullanıcı tercihlerini de sil)
    public Mono<Void> deleteUser(String userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    // Kullanıcı tercihlerini sil
                    return userPreferenceService.deleteUserPreferences(userId)
                            .then(Mono.defer(() -> {
                                // MongoDB'den kullanıcıyı sil
                                return userRepository.deleteById(userId);
                            }))
                            .then(Mono.fromRunnable(() -> {
                                try {
                                    // Firebase'den kullanıcıyı sil
                                    firebaseAuth.deleteUser(userId);
                                } catch (FirebaseAuthException e) {
                                    log.error("Firebase'den kullanıcı silinirken hata: {}", e.getMessage());
                                    // Hata olsa bile işleme devam et
                                }
                                // Kafka olaylarını gönder
                                kafkaService.sendUserDeletedEvent(user);
                                sendUserEvent(user, "USER_DELETED");
                                log.info("Kullanıcı başarıyla silindi: {}", userId);
                            }));
                })
                .then();
    }

    // Firebase kimlik doğrulama ve senkronizasyon metodunu güncelleme
    public Mono<UserEntity> verifyAndCreateOrUpdateUser(String firebaseToken) {
        return verifyFirebaseToken(firebaseToken)
                .flatMap(decodedToken -> userRepository.findById(decodedToken.getUid())
                        .flatMap(existingUser -> {
                            // Mevcut kullanıcıyı güncelle
                            boolean needsUpdate = false;
                            
                            if (!existingUser.getEmail().equals(decodedToken.getEmail())) {
                                existingUser.setEmail(decodedToken.getEmail());
                                needsUpdate = true;
                            }
                            
                            if (decodedToken.getName() != null && 
                                (existingUser.getDisplayName() == null || 
                                !existingUser.getDisplayName().equals(decodedToken.getName()))) {
                                existingUser.setDisplayName(decodedToken.getName());
                                needsUpdate = true;
                            }
                            
                            if (decodedToken.getPicture() != null && 
                                (existingUser.getPhotoUrl() == null || 
                                !existingUser.getPhotoUrl().equals(decodedToken.getPicture()))) {
                                existingUser.setPhotoUrl(decodedToken.getPicture());
                                needsUpdate = true;
                            }
                            
                            if (needsUpdate) {
                                existingUser.setUpdatedAt(System.currentTimeMillis());
                                return userRepository.save(existingUser);
                            }
                            
                            return Mono.just(existingUser);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            // Kullanıcı yoksa yeni oluştur
                            return buildUserWithUniqueUsername(decodedToken)
                                    .flatMap(this::createUser);
                        })));
    }

    public Mono<UserEntity> findById(String id) {
        return cacheService.get(id)
                .flatMap(cachedJson -> {
                    // Convert cached JSON string to UserEntity
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        return Mono.just(objectMapper.readValue(cachedJson, UserEntity.class));
                    } catch (Exception e) {
                        log.error("Error deserializing cached user data: {}", e.getMessage());
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(userRepository.findById(id)
                        .flatMap(user -> cacheService.set(id, user)
                                .thenReturn(user)));
    }

    // Bu metot kullanıcının plan bilgisini getirir
    // Gerçek uygulamada kullanıcı veritabanı veya üyelik servisi üzerinden sorgu yapılmalıdır
    public Mono<String> getUserPlan(String userId) {
        // Örnek olarak varsayılan değer döndürüyoruz
        // Gerçek uygulamada bu kullanıcı veritabanından veya başka bir servisten alınır
        return Mono.just("free");
    }
    
    public Mono<UserPreference> getUserPreferences(String userId) {
        return userPreferenceService.getUserPreferences(userId);
    }

    /**
     * Firebase token'ını doğrular
     * 
     * @param firebaseToken Firebase kimlik token'ı
     * @return Çözümlenmiş FirebaseToken
     */
    private Mono<FirebaseToken> verifyFirebaseToken(String firebaseToken) {
        return Mono.fromCallable(() -> {
            try {
                return firebaseAuth.verifyIdToken(firebaseToken);
            } catch (FirebaseAuthException e) {
                log.error("Token doğrulama hatası: {}", e.getMessage());
                throw new RuntimeException("Token doğrulama hatası: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Kullanıcı olay mesajını gönderir
     * 
     * @param user Kullanıcı varlığı
     * @param eventType Olay tipi (örn. USER_CREATED, USER_UPDATED)
     */
    private void sendUserEvent(UserEntity user, String eventType) {
        try {
            UserEvent event = UserEvent.fromEntity(user, eventType);
            kafkaTemplate.send(userEventsTopic, user.getId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kullanıcı olayı gönderilirken hata: {}", ex.getMessage());
                    } else {
                        log.debug("Kullanıcı olayı gönderildi: userId={}, event={}", user.getId(), eventType);
                    }
                });
        } catch (Exception e) {
            log.error("Kullanıcı olayı gönderilirken beklenmeyen hata: {}", e.getMessage());
        }
    }

    /**
     * Kullanıcı alanlarını güncellemek için kullanılan yardımcı metot
     * 
     * @param existingUser Mevcut kullanıcı
     * @param updates Güncellenecek değerler
     */
    private void updateUserFields(UserEntity existingUser, UserEntity updates) {
        if (updates.getEmail() != null) {
            existingUser.setEmail(updates.getEmail());
        }
        if (updates.getUsername() != null) {
            existingUser.setUsername(updates.getUsername());
        }
        if (updates.getDisplayName() != null) {
            existingUser.setDisplayName(updates.getDisplayName());
        }
        if (updates.getPhotoUrl() != null) {
            existingUser.setPhotoUrl(updates.getPhotoUrl());
        }
        if (updates.getRole() != null) {
            existingUser.setRole(updates.getRole());
        }
        if (updates.getStatus() != null) {
            existingUser.setStatus(updates.getStatus());
        }
        
        existingUser.setUpdatedAt(System.currentTimeMillis());
    }

    /**
     * Kullanıcı durumunu günceller
     * 
     * @param userId Kullanıcı ID
     * @param status Yeni durum
     * @return Güncellenmiş kullanıcı
     */
    public Mono<UserEntity> updateUserStatus(String userId, UserStatus status) {
        log.info("Kullanıcı durumu güncelleniyor: id={}, status={}", userId, status);
        return userRepository.findById(userId)
                .flatMap(existingUser -> {
                    existingUser.setStatus(status);
                    existingUser.setUpdatedAt(System.currentTimeMillis());
                    return userRepository.save(existingUser);
                })
                .doOnSuccess(updatedUser -> {
                    kafkaService.sendUserUpdatedEvent(updatedUser);
                    sendUserEvent(updatedUser, "USER_STATUS_UPDATED");
                    log.info("Kullanıcı durumu başarıyla güncellendi: id={}, status={}", userId, status);
                })
                .doOnError(e -> log.error("Kullanıcı durumu güncellenirken hata: id={}, error={}", userId, e.getMessage()));
    }

    /**
     * Email veya kullanıcı adına göre kullanıcı arar
     * 
     * @param email Email adresi
     * @param username Kullanıcı adı
     * @return Bulunan kullanıcı veya hata
     */
    public Mono<UserEntity> searchUsers(String email, String username) {
        log.info("Kullanıcı aranıyor: email={}, username={}", email, username);
        
        if (email != null && !email.trim().isEmpty()) {
            return userRepository.findByEmail(email)
                    .switchIfEmpty(Mono.error(new UserNotFoundException("Email ile kullanıcı bulunamadı: " + email)));
        } else if (username != null && !username.trim().isEmpty()) {
            return userRepository.findByUsername(username)
                    .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı adı ile kullanıcı bulunamadı: " + username)));
        } else {
            return Mono.error(new ValidationException("Arama için email veya username parametresi gereklidir."));
        }
    }

    /**
     * Firebase'den gelen güncellemeleri sistemle senkronize eder
     * 
     * @param userId Kullanıcı ID
     * @param updates Güncellemeler
     * @return Güncellenmiş kullanıcı
     */
    public Mono<UserEntity> handleFirebaseUpdate(String userId, UserEntity updates) {
        log.info("Firebase güncellemesi işleniyor: id={}", userId);
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(existingUser -> {
                    updateUserFields(existingUser, updates);
                    return userRepository.save(existingUser);
                })
                .doOnSuccess(updatedUser -> {
                    kafkaService.sendUserUpdatedEvent(updatedUser);
                    sendUserEvent(updatedUser, "USER_FIREBASE_SYNC");
                    log.info("Firebase güncellemesi başarıyla işlendi: id={}", userId);
                })
                .doOnError(e -> log.error("Firebase güncellemesi işlenirken hata: id={}, error={}", userId, e.getMessage()));
    }
}
