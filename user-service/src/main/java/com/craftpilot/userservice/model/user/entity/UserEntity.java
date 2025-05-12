package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.user.enums.UserRole;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String photoUrl;
    private UserRole role;
    private UserStatus status;
    private long createdAt;
    private long updatedAt;
}
