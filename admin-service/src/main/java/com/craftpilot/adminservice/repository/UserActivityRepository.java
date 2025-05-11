package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.UserActivity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface UserActivityRepository extends ReactiveMongoRepository<UserActivity, String> {
    
    Flux<UserActivity> findByUserId(String userId);
    
    Flux<UserActivity> findByActivityType(UserActivity.ActivityType activityType);
    
    Flux<UserActivity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Flux<UserActivity> findByStatus(UserActivity.ActivityStatus status);
    
    Flux<UserActivity> findByUserIdAndActivityType(String userId, UserActivity.ActivityType activityType);
    
    Flux<UserActivity> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    Flux<UserActivity> findByServiceId(String serviceId);
}