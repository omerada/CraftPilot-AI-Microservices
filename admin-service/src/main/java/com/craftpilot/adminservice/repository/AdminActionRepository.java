package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.AdminAction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface AdminActionRepository extends ReactiveMongoRepository<AdminAction, String> {
    
    Flux<AdminAction> findByAdminId(String adminId);
    
    Flux<AdminAction> findByActionType(AdminAction.ActionType actionType);
    
    Flux<AdminAction> findByStatus(AdminAction.ActionStatus status);
    
    Flux<AdminAction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Flux<AdminAction> findByTargetId(String targetId);
    
    Flux<AdminAction> findByTargetIdAndActionType(String targetId, AdminAction.ActionType actionType);
    
    Flux<AdminAction> findByAdminIdAndTimestampBetween(String adminId, LocalDateTime start, LocalDateTime end);
    
    Flux<AdminAction> findByTargetTypeAndStatus(String targetType, AdminAction.ActionStatus status);
}