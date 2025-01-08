package com.craftpilot.userservice.service.impl;

import com.craftpilot.userservice.base.AbstractBaseServiceTest;
import com.craftpilot.userservice.builder.UserEntityBuilder;
import com.craftpilot.userservice.exception.PasswordNotValidException;
import com.craftpilot.userservice.exception.UserNotFoundException;
import com.craftpilot.userservice.model.user.Token;
import com.craftpilot.userservice.model.user.dto.request.LoginRequest;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.repository.UserRepository;
import com.craftpilot.userservice.service.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserLoginServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private UserLoginServiceImpl userLoginService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Test
    void login_ValidCredentials_ReturnsToken() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        UserEntity userEntity = new UserEntityBuilder().withValidUserFields().build();

        Token expectedToken = Token.builder()
                .accessToken("mockAccessToken")
                .accessTokenExpiresAt(123456789L)
                .refreshToken("mockRefreshToken")
                .build();

        // When
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(userEntity));

        when(passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword()))
                .thenReturn(true);

        when(tokenService.generateToken(userEntity.getClaims())).thenReturn(expectedToken);

        Token actualToken = userLoginService.login(loginRequest);

        // Then
        assertEquals(expectedToken.getAccessToken(), actualToken.getAccessToken());
        assertEquals(expectedToken.getRefreshToken(), actualToken.getRefreshToken());
        assertEquals(expectedToken.getAccessTokenExpiresAt(), actualToken.getAccessTokenExpiresAt());

        // Verify
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), userEntity.getPassword());
        verify(tokenService).generateToken(userEntity.getClaims());

    }

    @Test
    void login_InvalidEmail_ThrowsAdminNotFoundException() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        // When
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userLoginService.login(loginRequest));

        assertEquals("User not found!\n Can't find with given email: " + loginRequest.getEmail(), exception.getMessage());

        // Verify
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verifyNoInteractions(passwordEncoder, tokenService);

    }

    @Test
    void login_InvalidPassword_ThrowsPasswordNotValidException() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("invalidPassword")
                .build();

        UserEntity userEntity = new UserEntityBuilder()
                .withEmail(loginRequest.getEmail())
                .withPassword("encodedPassword")
                .build();

        // When
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(userEntity));

        when(passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword()))
                .thenReturn(false);

        // Then
        PasswordNotValidException exception = assertThrows(PasswordNotValidException.class,
                () -> userLoginService.login(loginRequest));

        assertNotNull(exception);

        // Verify
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), userEntity.getPassword());
        verifyNoInteractions(tokenService);

    }
}