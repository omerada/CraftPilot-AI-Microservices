package com.craftpilot.activitylogservice.repository;

import com.craftpilot.activitylogservice.model.ActivityLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends ReactiveMongoRepository<ActivityLog, String> {
    
    Flux<ActivityLog> findByUserId(String userId);
    
    Flux<ActivityLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    Flux<ActivityLog> findByServiceName(String serviceName);
}
