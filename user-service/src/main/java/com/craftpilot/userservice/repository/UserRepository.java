package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);
    Mono<User> findByUid(String uid);
    Mono<Boolean> existsByEmail(String email);
}
