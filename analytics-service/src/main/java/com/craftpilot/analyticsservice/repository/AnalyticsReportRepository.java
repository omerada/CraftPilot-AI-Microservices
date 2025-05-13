package com.craftpilot.analyticsservice.repository;

import com.craftpilot.analyticsservice.model.AnalyticsReport;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface AnalyticsReportRepository extends ReactiveMongoRepository<AnalyticsReport, String> {
    
    Flux<AnalyticsReport> findByType(AnalyticsReport.ReportType type);
    
    Flux<AnalyticsReport> findByStatus(AnalyticsReport.ReportStatus status);
    
    Flux<AnalyticsReport> findByCreatedBy(String userId);
    
    @Query("{ 'reportStartTime': { $gte: ?0 }, 'reportEndTime': { $lte: ?1 } }")
    Flux<AnalyticsReport> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'reportStartTime': { $gte: ?0 } }")
    Flux<AnalyticsReport> findByReportStartTimeAfter(LocalDateTime startTime);
    
    @Query("{ 'reportEndTime': { $lte: ?0 } }")
    Flux<AnalyticsReport> findByReportEndTimeBefore(LocalDateTime endTime);
    
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    Flux<AnalyticsReport> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'type': ?0, 'status': ?1 }")
    Flux<AnalyticsReport> findByTypeAndStatus(AnalyticsReport.ReportType type, AnalyticsReport.ReportStatus status);
    
    @Query("{ 'createdBy': ?0, 'type': ?1 }")
    Flux<AnalyticsReport> findByCreatedByAndType(String userId, AnalyticsReport.ReportType type);
    
    @Query("{ 'createdBy': ?0, 'status': ?1 }")
    Flux<AnalyticsReport> findByCreatedByAndStatus(String userId, AnalyticsReport.ReportStatus status);
    
    @Query("{ 'tags': { $in: [?0] } }")
    Flux<AnalyticsReport> findByTag(String tag);
}