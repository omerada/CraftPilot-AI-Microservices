package com.craftpilot.userservice.model.user.entity;

import com.craftpilot.userservice.model.common.entity.BaseEntity;
import com.craftpilot.userservice.model.user.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user entity named {@link UserEntity} in the system.
 * This entity stores user-related information such as email, password, and personal details.
 */
@Entity
@Table(name = "users", schema = "user_service")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "username")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type")
    private MembershipType membershipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Constructs a map of claims based on the user's attributes.
     * This map is typically used to create JWT claims for the user.
     * @return a map of claims containing user attributes
     */
    public Map<String, Object> getClaims() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(TokenClaims.USER_ID.getValue(), this.id);
        claims.put(TokenClaims.USER_TYPE.getValue(), this.userType);
        claims.put(TokenClaims.USER_STATUS.getValue(), this.userStatus);
        claims.put(TokenClaims.USER_NAME.getValue(), this.username);
        claims.put(TokenClaims.USER_EMAIL.getValue(), this.email);
        return claims;
    }
}


