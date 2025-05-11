package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<UserEntity, String> {
    Mono<UserEntity> findByEmail(String email);
    Mono<UserEntity> findByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
}
