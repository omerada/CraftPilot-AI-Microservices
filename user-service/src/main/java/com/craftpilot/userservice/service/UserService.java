package com.craftpilot.userservice.service;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.redis.core.RedisTemplate;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.repository.UserRepository;
import com.craftpilot.userservice.event.UserEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 

/**
 * Kullanıcı işlemleri için servis arayüzü.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "users")
public class UserService {

    private final UserRepository userRepository; 
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String CIRCUIT_BREAKER_NAME = "userService";
    private static final String CACHE_KEY_PREFIX = "users::";
    private static final String PROFILE_CACHE_KEY_PREFIX = "userProfiles::";

    /**
     * E-posta ile kullanıcı arama işlemi.
     *
     * @param email Kullanıcı e-posta adresi
     * @return E-posta ile bulunan kullanıcı nesnesi
     */
    @Cacheable(key = "#email", unless = "#result == null")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "findByEmailFallback")
    public UserEntity findByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Email ile kullanıcı bulunamadı: " + email));
    }

    public UserEntity findByEmailFallback(String email, Exception ex) {
        log.error("Fallback: Error fetching user by email: {}", email, ex);
        try {
            return (UserEntity) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + "email:" + email);
        } catch (Exception e) {
            log.error("Redis fallback error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kullanıcı oluşturma işlemi.
     *
     * @param userEntity Oluşturulacak kullanıcı bilgileri
     * @return Oluşturulan kullanıcı nesnesi
     */
    public UserEntity createUser(UserEntity userEntity) {
        // Implementation of createUser method
        return null; // Placeholder return, actual implementation needed
    }

    /**
     * Kullanıcı güncelleme işlemi.
     *
     * @param userId     Güncellenecek kullanıcının ID'si
     * @param userEntity Güncellenmiş kullanıcı bilgileri
     * @return Güncellenen kullanıcı nesnesi
     */
    public UserEntity updateUser(String userId, UserEntity userEntity) {
        // Implementation of updateUser method
        return null; // Placeholder return, actual implementation needed
    }

    /**
     * Kullanıcı profili bilgilerini Firestore'dan alma işlemi.
     *
     * @param userId Kullanıcı ID'si
     * @return Kullanıcı profili bilgileri
     */
    public UserEntity getUserDetailsFromFirestore(String userId) {
        // Implementation of getUserDetailsFromFirestore method
        return null; // Placeholder return, actual implementation needed
    }

    @Cacheable(cacheNames = "userProfiles", key = "#uid", unless = "#result == null")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserProfileFallback")
    public UserEntity getUserProfile(String uid) {
        log.debug("Fetching user profile for uid: {}", uid);
        var user = userRepository.findById(uid)
            .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı: " + uid));
            
        publishUserEvent(uid, user.getEmail(), UserEvent.UserEventType.PROFILE_VIEWED);
        return user;
    }

    public UserEntity getUserProfileFallback(String uid, Exception ex) {
        log.error("Fallback: Error fetching user profile: {}", uid, ex);
        try {
            return (UserEntity) redisTemplate.opsForValue().get(PROFILE_CACHE_KEY_PREFIX + uid);
        } catch (Exception e) {
            log.error("Redis fallback error: {}", e.getMessage());
            return null;
        }
    }

    @CacheEvict(cacheNames = {"users", "userProfiles"}, key = "#uid")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    @Transactional
    public void updateUserProfile(String uid, UserEntity updatedUser) {
        log.debug("Updating user profile for uid: {}", uid);
        var existingUser = userRepository.findById(uid)
            .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı: " + uid));

        updatedUser.setUid(uid);
        updatedUser.setUpdatedInfo(uid);
        
        userRepository.save(updatedUser);
        publishUserEvent(uid, updatedUser.getEmail(), UserEvent.UserEventType.UPDATED);
        updateUserCache(updatedUser);
    }

    @Transactional
    public void createUserInFirestore(UserEntity user) {
        log.debug("Creating new user in Firestore: {}", user.getEmail());
        try {
            userRepository.save(user);
            updateUserCache(user);
        } catch (Exception e) {
            log.error("Kullanıcı kayıt hatası: {}", e.getMessage());
            throw new RuntimeException("Kullanıcı kayıt hatası", e);
        }
    }

    private void updateUserCache(UserEntity user) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + "email:" + user.getEmail(), user);
            redisTemplate.opsForValue().set(PROFILE_CACHE_KEY_PREFIX + user.getUid(), user);
        } catch (Exception e) {
            log.error("Cache update error for user: {}, error: {}", user.getEmail(), e.getMessage());
        }
    }

    private void publishUserEvent(String uid, String email, UserEvent.UserEventType eventType) {
        try {
            kafkaTemplate.send(USER_EVENTS_TOPIC, new UserEvent(uid, email, eventType));
        } catch (Exception e) {
            log.error("Event publishing error for user: {}, event: {}, error: {}", 
                     uid, eventType, e.getMessage());
        }
    }
}
