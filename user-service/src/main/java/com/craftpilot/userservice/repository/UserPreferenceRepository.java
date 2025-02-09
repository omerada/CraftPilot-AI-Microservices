package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserPreferenceRepository {
    Mono<UserPreference> findById(String id);
    Mono<UserPreference> findByUserId(String userId);
    Mono<UserPreference> save(UserPreference preference);
}