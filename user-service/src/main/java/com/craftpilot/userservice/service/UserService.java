package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserDTO;
import com.craftpilot.userservice.mapper.UserMapper;
import com.craftpilot.userservice.model.User;
import com.craftpilot.userservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.exception.AuthenticationException;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;

    /**
     * Token doğrulama ve kullanıcı getirme işlemini MongoDB kullanarak yapar
     * 
     * @param token Firebase ID token
     * @return User objesi
     */
    public Mono<User> verifyTokenAndGetUser(String token) {
        return Mono.fromCallable(() -> {
            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                return decodedToken.getUid();
            } catch (FirebaseAuthException e) {
                log.error("Firebase token doğrulama hatası: {}", e.getMessage());
                throw new AuthenticationException("Geçersiz veya süresi dolmuş token: " + e.getMessage());
            }
        }).flatMap(uid ->
        // Firestore sorgusu yerine MongoDB repository kullanıyoruz
        userRepository.findByUid(uid)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, UID: " + uid))));
    }

    /**
     * Kullanıcı oluşturma - MongoDB repository kullanarak
     */
    public Mono<User> createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Kullanıcıyı güncelleme - MongoDB repository kullanarak
     */
    public Mono<User> updateUser(String userId, User userUpdates) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, ID: " + userId)))
                .flatMap(existingUser -> {
                    // Mevcut kullanıcının güncellenecek alanlarını güncelle
                    if (userUpdates.getDisplayName() != null) {
                        existingUser.setDisplayName(userUpdates.getDisplayName());
                    }
                    if (userUpdates.getPhotoUrl() != null) {
                        existingUser.setPhotoUrl(userUpdates.getPhotoUrl());
                    }
                    // Diğer alanlar...

                    existingUser.setLastUpdatedAt(LocalDateTime.now());
                    return userRepository.save(existingUser);
                });
    }

    /**
     * Tüm kullanıcıları getir - MongoDB repository kullanarak
     */
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Kullanıcı bilgilerini getir - MongoDB repository kullanarak
     */
    public Mono<User> getUserById(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, ID: " + id)));
    }

    /**
     * Firebase UID ile kullanıcı getir - MongoDB repository kullanarak
     */
    public Mono<User> getUserByUid(String uid) {
        return userRepository.findByUid(uid)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Kullanıcı bulunamadı, UID: " + uid)));
    }
}
