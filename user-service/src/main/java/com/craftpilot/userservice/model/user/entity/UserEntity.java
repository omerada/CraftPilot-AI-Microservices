package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Temel kullanıcı bilgilerini tutan entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    
    private String id;
    
    private String username;
    private String displayName;
    private String photoUrl;
    private String email;
    private UserStatus status;
    private long createdAt;
    private long updatedAt;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    private String firestoreId;
    private String path;
    private Object customClaims;
    private Object metadata;
}
