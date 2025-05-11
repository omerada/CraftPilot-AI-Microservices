package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.AuditLog;
import com.craftpilot.adminservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public Mono<AuditLog> logActivity(AuditLog auditLog) {
        log.info("Logging activity for user: {}", auditLog.getUserId());
        return auditLogRepository.save(auditLog);
    }

    public Mono<AuditLog> getLogById(String id) {
        log.info("Retrieving audit log with ID: {}", id);
        return auditLogRepository.findById(id);
    }

    public Flux<AuditLog> getLogsByUser(String userId) {
        log.info("Retrieving audit logs for user: {}", userId);
        return auditLogRepository.findByUserId(userId);
    }

    public Flux<AuditLog> getLogsByService(String serviceId) {
        log.info("Retrieving audit logs for service: {}", serviceId);
        return auditLogRepository.findByServiceId(serviceId);
    }

    public Flux<AuditLog> getLogsByType(AuditLog.LogType logType) {
        log.info("Retrieving audit logs of type: {}", logType);
        return auditLogRepository.findByLogType(logType);
    }

    public Flux<AuditLog> getLogsByStatus(AuditLog.LogStatus status) {
        log.info("Retrieving audit logs with status: {}", status);
        return auditLogRepository.findByStatus(status);
    }

    public Flux<AuditLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("Retrieving audit logs between {} and {}", start, end);
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    public Flux<AuditLog> getLogsByResource(String resource) {
        log.info("Retrieving audit logs for resource: {}", resource);
        return auditLogRepository.findByResource(resource);
    }

    public Mono<Void> deleteLog(String id) {
        log.info("Deleting audit log with ID: {}", id);
        return auditLogRepository.deleteById(id);
    }

    public Mono<List<AuditLog>> getSecurityLogs() {
        log.info("Retrieving security audit logs");
        return auditLogRepository.findByLogType(AuditLog.LogType.SECURITY)
                .collectList();
    }

    public Mono<List<AuditLog>> getErrorLogs() {
        log.info("Retrieving error audit logs");
        return auditLogRepository.findByLogType(AuditLog.LogType.ERROR)
                .collectList();
    }

    public Mono<List<AuditLog>> getRecentLogs(LocalDateTime since) {
        log.info("Retrieving recent audit logs since {}", since);
        return auditLogRepository.findByTimestampBetween(since, LocalDateTime.now())
                .collectList();
    }

    public Mono<AuditLog> logSecurityEvent(String userId, String action, String resource, Map<String, Object> details) {
        log.info("Logging security event for user: {}", userId);
        AuditLog securityLog = AuditLog.builder()
                .userId(userId)
                .logType(AuditLog.LogType.SECURITY)
                .action(action)
                .resource(resource)
                .requestData(details)
                .status(AuditLog.LogStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return auditLogRepository.save(securityLog);
    }

    public Mono<AuditLog> logOperationalEvent(String serviceId, String action, Map<String, Object> operationDetails) {
        log.info("Logging operational event for service: {}", serviceId);
        AuditLog operationalLog = AuditLog.builder()
                .serviceId(serviceId)
                .logType(AuditLog.LogType.OPERATION)
                .action(action)
                .requestData(operationDetails)
                .status(AuditLog.LogStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return auditLogRepository.save(operationalLog);
    }

    public Mono<AuditLog> logErrorEvent(String serviceId, String action, String errorDetails, Map<String, Object> context) {
        log.info("Logging error event for service: {}", serviceId);
        AuditLog errorLog = AuditLog.builder()
                .serviceId(serviceId)
                .logType(AuditLog.LogType.ERROR)
                .action(action)
                .requestData(context)
                .responseData(Map.of("error", errorDetails))
                .status(AuditLog.LogStatus.ERROR)
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return auditLogRepository.save(errorLog);
    }

    public Mono<Long> getLogCount(String userId) {
        log.info("Counting audit logs for user: {}", userId);
        return auditLogRepository.findByUserId(userId)
                .count();
    }
}