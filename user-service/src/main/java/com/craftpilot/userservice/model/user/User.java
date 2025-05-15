package com.craftpilot.userservice.model.user;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String displayName;
    private String photoUrl;
    private String phoneNumber;
    private boolean emailVerified;
    private boolean disabled;
    private Map<String, Object> customClaims;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    public static User fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .displayName(entity.getDisplayName())
                .photoUrl(entity.getPhotoUrl())
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .createdAt(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(entity.getCreatedAt()), 
                    ZoneOffset.UTC))
                .updatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(entity.getUpdatedAt()), 
                    ZoneOffset.UTC))
                .build();
    }
}
