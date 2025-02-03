package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.AdminAction;
import com.craftpilot.adminservice.repository.AdminActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActionService {
    private final AdminActionRepository adminActionRepository;

    public Mono<AdminAction> recordAction(AdminAction action) {
        log.info("Recording admin action by admin: {}", action.getAdminId());
        return adminActionRepository.save(action);
    }

    public Mono<AdminAction> getActionById(String id) {
        log.info("Retrieving admin action with ID: {}", id);
        return adminActionRepository.findById(id);
    }

    public Flux<AdminAction> getActionsByAdmin(String adminId) {
        log.info("Retrieving actions for admin: {}", adminId);
        return adminActionRepository.findByAdminId(adminId);
    }

    public Flux<AdminAction> getActionsByType(AdminAction.ActionType actionType) {
        log.info("Retrieving actions of type: {}", actionType);
        return adminActionRepository.findByActionType(actionType);
    }

    public Flux<AdminAction> getActionsByStatus(AdminAction.ActionStatus status) {
        log.info("Retrieving actions with status: {}", status);
        return adminActionRepository.findByStatus(status);
    }

    public Flux<AdminAction> getActionsByTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("Retrieving actions between {} and {}", start, end);
        return adminActionRepository.findByTimeRange(start, end);
    }

    public Flux<AdminAction> getActionsByTarget(String targetId) {
        log.info("Retrieving actions for target: {}", targetId);
        return adminActionRepository.findByTargetId(targetId);
    }

    public Mono<Void> deleteAction(String id) {
        log.info("Deleting action with ID: {}", id);
        return adminActionRepository.deleteById(id);
    }

    public Mono<AdminAction> updateActionStatus(String id, AdminAction.ActionStatus newStatus) {
        log.info("Updating action status for ID {} to {}", id, newStatus);
        return adminActionRepository.findById(id)
                .flatMap(action -> {
                    action.setStatus(newStatus);
                    return adminActionRepository.save(action);
                });
    }

    public Mono<List<AdminAction>> getPendingActions() {
        log.info("Retrieving pending actions");
        return adminActionRepository.findByStatus(AdminAction.ActionStatus.PENDING)
                .collectList();
    }

    public Mono<List<AdminAction>> getRecentActions(LocalDateTime since) {
        log.info("Retrieving recent actions since {}", since);
        return adminActionRepository.findByTimeRange(since, LocalDateTime.now())
                .collectList();
    }

    public Mono<Long> getActionCount(String adminId) {
        log.info("Counting actions for admin: {}", adminId);
        return adminActionRepository.findByAdminId(adminId)
                .count();
    }

    public Mono<List<AdminAction>> getFailedActions() {
        log.info("Retrieving failed actions");
        return adminActionRepository.findByStatus(AdminAction.ActionStatus.REJECTED)
                .collectList();
    }

    public Mono<Boolean> hasPermission(String adminId, AdminAction.ActionType actionType) {
        log.info("Checking permission for admin {} to perform action {}", adminId, actionType);
        // Bu metod, yetkilendirme servisine bağlanarak izinleri kontrol edebilir
        // Şimdilik basit bir kontrol yapıyoruz
        return Mono.just(true);
    }
} 