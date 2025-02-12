package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends ReactiveCrudRepository<UserPreference, String> {
    // Spring Data Redis will implement this interface automatically
}