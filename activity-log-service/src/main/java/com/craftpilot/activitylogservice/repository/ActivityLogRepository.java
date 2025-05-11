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
    
    Flux<ActivityLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    Flux<ActivityLog> findByServiceName(String serviceName);
    
    /**
     * Filtrelere göre kayıt sayısını döndürür
     */
    @Query(value = "{ " +
                  "$and: [" +
                    "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
                    "{ 'actionType': { $regex: ?1, $options: 'i' } }, " +
                    "{ $or: [ { 'timestamp': { $gte: ?2 } }, { ?2: null } ] }, " +
                    "{ $or: [ { 'timestamp': { $lte: ?3 } }, { ?3: null } ] }" +
                  "] }", 
            count = true)
    Mono<Long> countByFilters(String userId, String actionType, Date fromDate, Date toDate);
    
    /**
     * Filtrelere ve sayfalama parametrelerine göre logları döndürür
     */
    @Query("{ " +
           "$and: [" +
             "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
             "{ 'actionType': { $regex: ?1, $options: 'i' } }, " +
             "{ $or: [ { 'timestamp': { $gte: ?2 } }, { ?2: null } ] }, " +
             "{ $or: [ { 'timestamp': { $lte: ?3 } }, { ?3: null } ] }" +
           "] }")
    Flux<ActivityLog> findByFilters(String userId, String actionType, Date fromDate, Date toDate, Pageable pageable);
}
