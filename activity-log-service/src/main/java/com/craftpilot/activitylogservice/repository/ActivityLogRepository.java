package com.craftpilot.activitylogservice.repository;

import com.craftpilot.activitylogservice.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;

@Repository
public interface ActivityLogRepository extends ReactiveMongoRepository<ActivityLog, String> {
    
    Flux<ActivityLog> findByUserId(String userId);
    
    Flux<ActivityLog> findByActionType(String actionType);
    
    @Query("{ 'userId': ?0, 'actionType': ?1 }")
    Flux<ActivityLog> findByUserIdAndActionType(String userId, String actionType);
    
    @Query("{ 'eventTime': { $gte: ?0, $lte: ?1 } }")
    Flux<ActivityLog> findByEventTimeBetween(Date fromDate, Date toDate);
    
    @Query("{ $and: [ " +
           "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
           "{ 'actionType': { $regex: ?1, $options: 'i' } }, " +
           "{ 'eventTime': { $gte: ?2 } }, " +
           "{ 'eventTime': { $lte: ?3 } } " +
           "] }")
    Flux<ActivityLog> findByFilters(String userId, String actionType, Date fromDate, Date toDate, Pageable pageable);
    
    @Query(value = "{ $and: [ " +
            "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
            "{ 'actionType': { $regex: ?1, $options: 'i' } }, " +
            "{ 'eventTime': { $gte: ?2 } }, " +
            "{ 'eventTime': { $lte: ?3 } } " +
            "] }", count = true)
    Mono<Long> countByFilters(String userId, String actionType, Date fromDate, Date toDate);
}
