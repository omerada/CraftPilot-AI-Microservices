package com.craftpilot.userservice.repository;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.enums.UserStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<UserEntity, String> {
    
    Mono<UserEntity> findByUsername(String username);
    
    Mono<UserEntity> findByEmail(String email);
    
    @Query("{ 'email': ?0, 'status': { $ne: 'DELETED' } }")
    Mono<UserEntity> findActiveByEmail(String email);
    
    Flux<UserEntity> findByStatus(UserStatus status);
    
    @Query("{ 'status': ?0, 'createdAt': { $gt: ?1 } }")
    Flux<UserEntity> findByStatusAndCreatedAtAfter(UserStatus status, Long timestamp);
    
    @Query("{ 'displayName': { $regex: ?0, $options: 'i' } }")
    Flux<UserEntity> findByDisplayNameContainingIgnoreCase(String displayName);
    
    @Query("{ $or: [ { 'email': { $regex: ?0, $options: 'i' } }, { 'username': { $regex: ?0, $options: 'i' } }, { 'displayName': { $regex: ?0, $options: 'i' } } ] }")
    Flux<UserEntity> searchUsers(String searchTerm);
}
