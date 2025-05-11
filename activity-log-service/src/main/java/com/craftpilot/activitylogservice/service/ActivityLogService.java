package com.craftpilot.activitylogservice.service;

import com.craftpilot.activitylogservice.exception.ValidationException;
import com.craftpilot.activitylogservice.model.ActivityEvent;
import com.craftpilot.activitylogservice.model.ActivityLog;
import com.craftpilot.activitylogservice.model.PageResponse;
import com.craftpilot.activitylogservice.repository.ActivityLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Service
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final MeterRegistry meterRegistry;
    private final Timer processEventTimer;
    private final Timer queryLogsTimer;

    @Value("${pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${pagination.max-page-size:100}")
    private int maxPageSize;

    public ActivityLogService(ActivityLogRepository activityLogRepository, MeterRegistry meterRegistry) {
        this.activityLogRepository = activityLogRepository;
        this.meterRegistry = meterRegistry;
        
        this.processEventTimer = Timer.builder("activity.log.process.event")
            .description("Time taken to process an activity event")
            .register(meterRegistry);
            
        this.queryLogsTimer = Timer.builder("activity.log.query")
            .description("Time taken to query activity logs")
            .register(meterRegistry);
    }

    public Mono<ActivityLog> processEvent(ActivityEvent event) {
        return Mono.just(event)
            .doOnNext(e -> log.info("Processing activity event: userId={}, actionType={}, metadata={}", 
                    e.getUserId(), e.getActionType(), e.getMetadata()))
            .filter(this::validateEvent)
            .switchIfEmpty(Mono.error(new ValidationException("Invalid activity event")))
            .map(this::convertToActivityLog)
            .flatMap(activityLogRepository::save)
            .doOnSuccess(savedLog -> {
                log.info("Activity logged: userId={}, actionType={}, id={}", 
                    savedLog.getUserId(), savedLog.getActionType(), savedLog.getId());
                meterRegistry.counter("activity.log.saved", 
                    "actionType", savedLog.getActionType(), 
                    "userId", savedLog.getUserId()).increment();
            })
            .doOnError(error -> log.error("Failed to process activity event: {}", error.getMessage(), error))
            .name("processEvent")
            .tag("source", "kafka")
            .metrics();
    }
    
    private boolean validateEvent(ActivityEvent event) {
        boolean isValid = event.isValid();
        if (!isValid) {
            log.error("Invalid activity event received: {}", event);
        }
        return isValid;
    }
    
    private ActivityLog convertToActivityLog(ActivityEvent event) {
        ActivityLog log = ActivityLog.fromEvent(event);
        return log;
    }

    public Mono<PageResponse<ActivityLog>> getLogs(
            String userId, 
            String actionType, 
            String fromDate, 
            String toDate, 
            Integer page, 
            Integer size) {
        
        return Mono.defer(() -> {
            // Validate and parse input parameters
            final Date fromDateTime = parseDateTime(fromDate, null);
            final Date toDateTime = parseDateTime(toDate, null);
            
            // Null kontrolü ve boş string kontrolü
            final String effectiveUserId = (userId == null || userId.trim().isEmpty()) ? ".*" : userId;
            final String effectiveActionType = (actionType == null || actionType.trim().isEmpty()) ? ".*" : actionType;
            
            final int validatedPage = page != null && page >= 0 ? page : 0;
            final int validatedSize = validatePageSize(size);
            
            // MongoDB pagination için PageRequest kullanma
            PageRequest pageRequest = PageRequest.of(
                validatedPage, 
                validatedSize, 
                Sort.by(Sort.Direction.DESC, "eventTime")
            );
            
            // Count total elements for pagination metadata
            Mono<Long> countMono = activityLogRepository.countByFilters(
                effectiveUserId, effectiveActionType, fromDateTime, toDateTime);
            
            // Get the actual page of logs
            Mono<java.util.List<ActivityLog>> logsMono = activityLogRepository
                .findByFilters(effectiveUserId, effectiveActionType, fromDateTime, toDateTime, pageRequest)
                .collectList();
            
            // Combine into a PageResponse
            return Mono.zip(logsMono, countMono)
                .map(tuple -> PageResponse.of(
                    tuple.getT1(),  // content
                    validatedPage,  // page number
                    validatedSize,  // page size
                    tuple.getT2()   // total elements
                ))
                .doOnSubscribe(s -> log.debug("Querying logs: userId={}, actionType={}, fromDate={}, toDate={}, page={}, size={}", 
                    effectiveUserId, effectiveActionType, fromDate, toDate, validatedPage, validatedSize))
                .doOnSuccess(response -> log.debug("Found {} logs", response.getContent().size()));
        })
        .name("getLogs")
        .metrics();
    }
    
    private int validatePageSize(Integer size) {
        if (size == null || size <= 0) {
            return defaultPageSize;
        }
        return Math.min(size, maxPageSize);
    }
    
    private Date parseDateTime(String dateTimeStr, Date defaultValue) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}", dateTimeStr);
            return defaultValue;
        }
    }
}
