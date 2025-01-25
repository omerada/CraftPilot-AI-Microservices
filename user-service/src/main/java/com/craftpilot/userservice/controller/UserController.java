package com.craftpilot.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing user-related operations using Firebase Authentication and Firestore.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Retrieves user details from Firestore using the Firebase UID.
     *
     * @param uid the Firebase user ID to retrieve details for
     * @return a {@link ResponseEntity} containing the user details
     */
    @GetMapping("/profile")
    public ResponseEntity<UserEntity> getUserProfile(
            @RequestHeader("X-User-ID") String uid) {
        return ResponseEntity.ok(userService.getUserProfile(uid));
    }

    /**
     * Updates user profile in Firestore.
     *
     * @param uid the Firebase user ID to update profile for
     * @param updatedProfile the updated user profile
     * @return a {@link ResponseEntity} indicating the success of the profile update operation
     */
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @RequestHeader("X-User-ID") String uid,
            @RequestBody UserEntity updatedProfile) {
        userService.updateUserProfile(uid, updatedProfile);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<UserEntity> findByEmail(
            @RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
}
