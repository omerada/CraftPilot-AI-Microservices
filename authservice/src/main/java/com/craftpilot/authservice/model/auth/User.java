package com.craftpilot.authservice.model.auth;

import com.craftpilot.authservice.model.auth.enums.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user named {@link User} in the system.
 * This class contains information about the user's identity, contact details, status, and type.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class User {
    private String id;
    private String email;
    private String userName;
    private MembershipType membershipType;
    private JobType jobType;
    private Language language;
    private UserStatus userStatus;
    private UserType userType;
}
