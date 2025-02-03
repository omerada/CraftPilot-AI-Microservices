package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import com.google.cloud.firestore.annotation.DocumentId;
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
    
    @DocumentId
    private String id;
    
    private String email;
    private String username;
    private String displayName;
    private String photoUrl;
    private UserRole role;
    private UserStatus status;
    private Long createdAt;
    private Long updatedAt;
    private String phoneNumber;
    private Boolean emailVerified;
    private String locale;
    private String timezone;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    private String firestoreId;
    private String path;
    private Object customClaims;
    private Object metadata;
}
