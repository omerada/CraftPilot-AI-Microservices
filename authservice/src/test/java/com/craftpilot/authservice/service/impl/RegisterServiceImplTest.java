package com.craftpilot.authservice.service.impl;

import com.craftpilot.authservice.base.AbstractBaseServiceTest;
import com.craftpilot.authservice.client.UserServiceClient;
import com.craftpilot.authservice.model.auth.User;
import com.craftpilot.authservice.model.auth.dto.request.RegisterRequest;
import com.craftpilot.authservice.model.auth.enums.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

class RegisterServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private RegisterServiceImpl registerService;

    @Mock
    private UserServiceClient userServiceClient;

    @Test
    void registerUser_ValidRegisterRequest_ReturnsUser() {

        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("valid.email@example.com")
                .password("validPassword123")
                .name("John")
                .membership("PREMIUM")
                .job("TEACHER")
                .language("ENGLISH")
                .role("USER")
                .build();

        User expectedUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("valid.email@example.com")
                .name("John")
                .membershipType(MembershipType.PREMIUM)
                .jobType(JobType.TEACHER)
                .language(Language.ENGLISH)
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.USER)
                .build();

        // When
        when(userServiceClient.register(any(RegisterRequest.class)))
                .thenReturn(ResponseEntity.ok(expectedUser));

        // Then
        User result = registerService.registerUser(registerRequest);

        assertNotNull(result);
        assertEquals(expectedUser, result);

        // Verify
        verify(userServiceClient, times(1)).register(any(RegisterRequest.class));

    }

    @Test
    void registerUser_InvalidRegisterRequest_ReturnsNull() {

        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("invalid.email")
                .password("short")
                .name("")
                .membership("")
                .job("")
                .language("")
                .role("")
                .build();

        // When
        when(userServiceClient.register(any(RegisterRequest.class)))
                .thenReturn(ResponseEntity.badRequest().build());

        // Then
        User result = registerService.registerUser(registerRequest);

        assertNull(result);

        // Verify
        verify(userServiceClient, times(1)).register(any(RegisterRequest.class));

    }

}
