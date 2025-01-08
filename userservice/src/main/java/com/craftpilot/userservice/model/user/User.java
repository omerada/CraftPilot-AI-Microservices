package com.craftpilot.userservice.model.user;

import com.craftpilot.userservice.model.common.BaseDomainModel;
import com.craftpilot.userservice.model.user.enums.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user domain object named {@link User} in the system.
 * The {@code User} class is a domain model that contains user-related information such as
 * identification, contact details, status, type, and password. It extends {@link BaseDomainModel}
 */
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseDomainModel {
    private String id;
    private String email;
    private String userName;
    private MembershipType membershipType;
    private JobType jobType;
    private Language language;
    private UserStatus userStatus;
    private UserType userType;
    private String password;
}

