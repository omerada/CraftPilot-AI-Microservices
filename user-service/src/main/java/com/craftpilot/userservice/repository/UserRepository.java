package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.User;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByUid(String uid);

    Mono<User> findByEmail(String email);

    @Query("{ 'roles.?0': true }")
    Flux<User> findByRole(String role);

    @Query("{ 'favoriteModelIds': ?0 }")
    Flux<User> findByFavoriteModelId(String modelId);
}
