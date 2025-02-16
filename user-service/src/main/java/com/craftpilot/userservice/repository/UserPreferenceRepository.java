package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.UserPreference;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserPreferenceRepository {
    private final ReactiveRedisOperations<String, UserPreference> redisOperations;

    public UserPreferenceRepository(ReactiveRedisOperations<String, UserPreference> redisOperations) {
        this.redisOperations = redisOperations;
    }

    private static final String KEY_PREFIX = "user:preference:";

    public Mono<UserPreference> findById(String userId) {
        return redisOperations.opsForValue().get(KEY_PREFIX + userId);
    }

    public Mono<UserPreference> save(UserPreference preference) {
        return redisOperations.opsForValue()
                .set(KEY_PREFIX + preference.getUserId(), preference)
                .thenReturn(preference);
    }

    public Mono<Void> deleteById(String userId) {
        return redisOperations.opsForValue()
                .delete(KEY_PREFIX + userId)
                .then();
    }
}