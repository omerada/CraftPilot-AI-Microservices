package com.craftpilot.userservice.service.impl;

import com.craftpilot.userservice.exception.PasswordNotValidException;
import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.model.user.Token;
import com.craftpilot.userservice.model.user.dto.request.LoginRequest;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.event.EventPublisher;
import com.craftpilot.userservice.model.user.event.LoginEvent;
import com.craftpilot.userservice.model.user.event.UserCreatedEvent;
import com.craftpilot.userservice.model.user.event.UserUpdatedEvent;
import com.craftpilot.userservice.repository.UserRepository;
import com.craftpilot.userservice.service.TokenService;
import com.craftpilot.userservice.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EventPublisher eventPublisher;

    @Override
    public Token login(LoginRequest loginRequest) {
        // Kullanıcıyı e-posta ile bulma
        UserEntity user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Şifreyi doğrulama
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new PasswordNotValidException("Invalid password");
        }

        // JWT token oluşturma
        Token token = tokenService.generateToken(user.getClaims());

        // Login event'ini yayınla
        eventPublisher.publishEvent(new LoginEvent(user.getId(), user.getEmail(), token.getAccessToken()));

        return token;
    }

    @Override
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public UserEntity createUser(UserEntity userEntity) {
        // Kullanıcıyı veritabanına kaydetme
        UserEntity savedUser = userRepository.save(userEntity);

        // Kullanıcı oluşturulduğunda bir event yayınlama
        eventPublisher.publishEvent(new UserCreatedEvent(savedUser));

        return savedUser;
    }

    @Override
    public UserEntity updateUser(String userId, UserEntity userEntity) {
        // Kullanıcıyı güncelleme
        userEntity.setId(userId);
        UserEntity updatedUser = userRepository.save(userEntity);

        // Kullanıcı güncellendiğinde bir event yayınlama
        eventPublisher.publishEvent(new UserUpdatedEvent(updatedUser));

        return updatedUser;
    }
}
