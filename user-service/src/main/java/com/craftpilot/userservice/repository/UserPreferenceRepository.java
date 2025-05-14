package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends ReactiveMongoRepository<UserPreference, String> {
    // Gerekli metotlar ReactiveMongoRepository tarafından sağlanıyor
}
