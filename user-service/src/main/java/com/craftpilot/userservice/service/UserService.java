package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserDTO;
import com.craftpilot.userservice.mapper.UserMapper;
import com.craftpilot.userservice.model.User;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.craftpilot.userservice.repository.UserRepository;
import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.exception.AuthenticationException;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceService userPreferenceService;

    @Autowired
    public UserService(UserRepository userRepository, UserPreferenceService userPreferenceService) {
        this.userRepository = userRepository;
        this.userPreferenceService = userPreferenceService;
        log.info("UserService başlatıldı");
    }

    /**
     * Token doğrulama ve kullanıcı getirme işlemini MongoDB kullanarak yapar
     */
    public Mono<User> authenticateAndGetUser(String token) {
        log.warn("Klasik token doğrulama kullanılıyor - Firebase kaldırıldı");
        // Token doğrulama mantığını burada implemente edin (JWT, OAuth, vb.)
        // Örnek implementasyon:
        return Mono.defer(() -> {
            try {
                // Token'dan kullanıcı ID'sini çıkarın (bu örnek için basit bir yaklaşım)
                String userId = extractUserIdFromToken(token);
                return userRepository.findById(userId)
                    .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, ID: " + userId)));
            } catch (Exception e) {
                log.error("Token doğrulama hatası: {}", e.getMessage());
                throw new AuthenticationException("Geçersiz veya süresi dolmuş token: " + e.getMessage());
            }
        });
    }
    
    private String extractUserIdFromToken(String token) {
        // Burada JWT token parse etme, OAuth token doğrulama vb. işlemler yapılabilir
        // Bu örnek için basit bir implementasyon:
        if (token == null || token.isEmpty()) {
            throw new AuthenticationException("Token boş olamaz");
        }
        
        // NOT: Bu sadece bir örnektir. Gerçek projelerde güvenli bir token doğrulama mekanizması kullanın.
        return token.substring(0, Math.min(token.length(), 24)); // MongoDB ID için 24 karakter
    }

    /**
     * Kullanıcı oluşturma
     */
    public Mono<User> createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Kullanıcıyı güncelleme
     */
    public Mono<UserEntity> updateUser(String userId, UserEntity userUpdates) {
        return findById(userId)
                .flatMap(existingUser -> {
                    // Mevcut kullanıcının güncellenecek alanlarını güncelle
                    if (userUpdates.getDisplayName() != null) {
                        existingUser.setDisplayName(userUpdates.getDisplayName());
                    }
                    if (userUpdates.getPhotoUrl() != null) {
                        existingUser.setPhotoUrl(userUpdates.getPhotoUrl());
                    }
                    // Diğer alanlar...

                    existingUser.setUpdatedAt(System.currentTimeMillis());
                    return Mono.just(existingUser);
                });
    }

    /**
     * Tüm kullanıcıları getir
     */
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Kullanıcı bilgilerini getir
     */
    public Mono<User> getUserById(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, ID: " + id)));
    }

    /**
     * Kullanıcıyı kimlik doğrulama token'ı ile doğrula ve oluştur
     */
    public Mono<UserEntity> verifyAndCreateUser(String authToken) {
        log.info("Yeni kullanıcı oluşturma isteği doğrulanıyor");
        // Token doğrulama ve kullanıcı oluşturma işlemi
        UserEntity user = new UserEntity();
        user.setId(extractUserIdFromToken(authToken));
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());
        return Mono.just(user);
    }

    /**
     * ID ile kullanıcı arama
     */
    public Mono<UserEntity> findById(String id) {
        log.info("ID ile kullanıcı aranıyor: {}", id);
        UserEntity user = new UserEntity();
        user.setId(id);
        return Mono.just(user);
    }

    /**
     * Kullanıcı silme
     */
    public Mono<Void> deleteUser(String id) {
        log.info("Kullanıcı siliniyor: {}", id);
        return userRepository.deleteById(id)
            .then(userPreferenceService.deleteUserPreferences(id))
            .doOnSuccess(v -> log.info("Kullanıcı ve tercihleri başarıyla silindi: {}", id))
            .doOnError(e -> log.error("Kullanıcı silinirken hata oluştu: id={}, error={}", id, e.getMessage()));
    }

    /**
     * Kullanıcı durumunu güncelleme
     */
    public Mono<UserEntity> updateUserStatus(String id, UserStatus status) {
        log.info("Kullanıcı durumu güncelleniyor: {} -> {}", id, status);
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStatus(status);
        return Mono.just(user);
    }

    /**
     * Email veya kullanıcı adı ile kullanıcı arama
     */
    public Mono<UserEntity> searchUsers(String email, String username) {
        log.info("Kullanıcı aranıyor: email={}, username={}", email, username);
        UserEntity user = new UserEntity();
        if (email != null) {
            user.setEmail(email);
        }
        if (username != null) {
            user.setUsername(username);
        }
        return Mono.just(user);
    }

    /**
     * Kullanıcıyı doğrula ve oluştur veya güncelle
     */
    public Mono<UserEntity> verifyAndCreateOrUpdateUser(String authToken) {
        log.info("Kullanıcı doğrulanıyor ve oluşturuluyor/güncelleniyor");
        return verifyAndCreateUser(authToken);
    }

    /**
     * Firebase güncellemelerini işle
     */
    public Mono<UserEntity> handleFirebaseUpdate(String id, UserEntity updates) {
        log.info("Firebase güncellemeleri işleniyor: {}", id);
        updates.setId(id);
        return Mono.just(updates);
    }

    /**
     * Kullanıcı planını getir (abonelik bilgisi)
     */
    public Mono<String> getUserPlan(String userId) {
        // Basit bir örnek - gerçekte bu veritabanı veya başka bir hizmetten alınabilir
        return Mono.just("standard");
    }

    /**
     * Kullanıcı tercihlerini getir
     */
    public Mono<UserPreference> getUserPreferences(String userId) {
        return userPreferenceService.getUserPreferences(userId)
            .doOnError(e -> log.error("Kullanıcı tercihleri alınırken hata: userId={}, error={}", userId, e.getMessage()));
    }
}
