package com.craftpilot.activitylogservice.repository;

import com.craftpilot.activitylogservice.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends ReactiveMongoRepository<ActivityLog, String> {
    
    Flux<ActivityLog> findByUserId(String userId);
    
    Flux<ActivityLog> findByActionType(String actionType);
    
    @Query("{ timestamp: { $gte: ?0, $lte: ?1 } }")
    Flux<ActivityLog> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    Flux<ActivityLog> findByUserIdAndActionType(String userId, String actionType);
    
    @Query("{ userId: ?0, timestamp: { $gte: ?1, $lte: ?2 } }")
    Flux<ActivityLog> findByUserIdAndTimeRange(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query("{ actionType: ?0, timestamp: { $gte: ?1, $lte: ?2 } }")
    Flux<ActivityLog> findByActionTypeAndTimeRange(String actionType, LocalDateTime start, LocalDateTime end);
    
    // Filtreleme ve sayfalama için özel sorgu metodları
    @Query("{" +
           "   $and: [" +
           "       { $or: [ { userId: ?0 }, { ?0: null } ] }," +
           "       { $or: [ { actionType: ?1 }, { ?1: null } ] }," +
           "       { $or: [ { timestamp: { $gte: ?2 } }, { ?2: null } ] }," +
           "       { $or: [ { timestamp: { $lte: ?3 } }, { ?3: null } ] }" +
           "   ]" +
           "}")
    Flux<ActivityLog> findByFilters(String userId, String actionType, 
                                   LocalDateTime fromDate, LocalDateTime toDate, 
                                   Pageable pageable);
    
    @Query(value = "{" +
           "   $and: [" +
           "       { $or: [ { userId: ?0 }, { ?0: null } ] }," +
           "       { $or: [ { actionType: ?1 }, { ?1: null } ] }," +
           "       { $or: [ { timestamp: { $gte: ?2 } }, { ?2: null } ] }," +
           "       { $or: [ { timestamp: { $lte: ?3 } }, { ?3: null } ] }" +
           "   ]" +
           "}", 
           count = true)
    Mono<Long> countByFilters(String userId, String actionType, 
                             LocalDateTime fromDate, LocalDateTime toDate);
}
