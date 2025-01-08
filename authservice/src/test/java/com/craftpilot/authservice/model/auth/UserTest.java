package com.craftpilot.authservice.model.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.craftpilot.authservice.model.auth.enums.*;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void testUserBuilder_WithAllFields() {

        // Given
        String id = "12345";
        String email = "example@example.com";
        String name = "John";
        String lastName = "Doe";
        MembershipType membershipType = MembershipType.PREMIUM;
        JobType jobType = JobType.TEACHER;
        Language language = Language.ENGLISH;
        UserStatus userStatus = UserStatus.ACTIVE;
        UserType userType = UserType.ADMIN;

        // When
        User user = User.builder()
                .id(id)
                .email(email)
                .userName(name)
                .membershipType(membershipType)
                .jobType(jobType)
                .language(language)
                .userStatus(userStatus)
                .userType(userType)
                .build();

        // Then
        assertNotNull(user);
        assertEquals(id, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals(name, user.getUserName());
        assertEquals(membershipType, user.getMembershipType());
        assertEquals(jobType, user.getJobType());
        assertEquals(language, user.getLanguage());
        assertEquals(userStatus, user.getUserStatus());
        assertEquals(userType, user.getUserType());
    }

    @Test
    void testUserBuilder_DefaultValues() {

        // When
        User user = User.builder().build();

        // Then
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getUserName());
        assertNull(user.getMembershipType());
        assertNull(user.getJobType());
        assertNull(user.getLanguage());
        assertNull(user.getUserStatus());
        assertNull(user.getUserType());
    }

    @Test
    void testUserSettersAndGetters() {

        // Given
        User user = new User();

        // When
        user.setId("12345");
        user.setEmail("example@example.com");
        user.setUserName("John Doe");
        user.setMembershipType(MembershipType.PREMIUM);
        user.setJobType(JobType.TEACHER);
        user.setLanguage(Language.ENGLISH);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setUserType(UserType.ADMIN);

        // Then
        assertEquals("12345", user.getId());
        assertEquals("example@example.com", user.getEmail());
        assertEquals("John Doe", user.getUserName());
        assertEquals(MembershipType.PREMIUM, user.getMembershipType());
        assertEquals(JobType.TEACHER, user.getJobType());
        assertEquals(Language.ENGLISH, user.getLanguage());
        assertEquals(UserStatus.ACTIVE, user.getUserStatus());
        assertEquals(UserType.ADMIN, user.getUserType());
    }

    @Test
    void testUserEquality() {

        // Given
        User user1 = User.builder()
                .id("12345")
                .email("example@example.com")
                .userName("John Doe")
                .membershipType(MembershipType.PREMIUM)
                .jobType(JobType.TEACHER)
                .language(Language.ENGLISH)
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.ADMIN)
                .build();

        User user2 = User.builder()
                .id("12345")
                .email("example@example.com")
                .userName("John Doe")
                .membershipType(MembershipType.PREMIUM)
                .jobType(JobType.TEACHER)
                .language(Language.ENGLISH)
                .userStatus(UserStatus.PASSIVE)
                .userType(UserType.ADMIN)
                .build();

        User user3 = User.builder()
                .id("67890")
                .email("different@example.com")
                .userName("Jane Smith")
                .membershipType(MembershipType.BASIC)
                .jobType(JobType.ENGINEER)
                .language(Language.SPANISH)
                .userStatus(UserStatus.SUSPENDED)
                .userType(UserType.USER)
                .build();

        // When & Then
        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
    }
}
