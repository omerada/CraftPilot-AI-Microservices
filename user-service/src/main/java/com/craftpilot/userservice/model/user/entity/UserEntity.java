package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.common.entity.BaseEntity;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user entity named {@link UserEntity} in the system.
 * This entity stores user-related information such as email, password, and personal details.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    @DocumentId
    private String uid;           // Firebase Auth UID
    private String email;
    private String userName;
    private String profilePicture;
    private String jobType;       // DEVELOPER, DESIGNER, PRODUCT_MANAGER vs.
    private String userType;      // FREE, PREMIUM, ENTERPRISE
    private String userStatus;    // ACTIVE, INACTIVE, SUSPENDED 
    private String timezone;
    private String language;      // en, tr vs.
    private NotificationPreferences notificationPreferences;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences {
        private boolean email = true;
        private boolean push = true;
        private boolean marketing = false;
    }
}
