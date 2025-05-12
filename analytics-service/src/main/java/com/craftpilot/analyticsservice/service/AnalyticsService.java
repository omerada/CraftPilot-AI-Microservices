package com.craftpilot.analyticsservice.service;

import com.craftpilot.analyticsservice.model.AnalyticsReport;
import com.craftpilot.analyticsservice.model.PerformanceMetrics;
import com.craftpilot.analyticsservice.model.UsageMetrics;
import com.craftpilot.analyticsservice.repository.AnalyticsReportRepository;
import com.craftpilot.analyticsservice.repository.PerformanceMetricsRepository;
import com.craftpilot.analyticsservice.repository.UsageMetricsRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UsageMetricsRepository usageMetricsRepository;
    private final PerformanceMetricsRepository performanceMetricsRepository;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topics.analytics-events}")
    private String analyticsEventsTopic;

    // Usage Metrics
    public Mono<UsageMetrics> recordUsageMetrics(UsageMetrics metrics) {
        return usageMetricsRepository.save(metrics)
                .doOnSuccess(saved -> {
                    log.info("Recorded usage metrics for user: {}, service: {}", 
                            saved.getUserId(), saved.getServiceType());
                    meterRegistry.counter("analytics.usage.recorded").increment();
                });
    }

    public Flux<UsageMetrics> getUserMetrics(String userId) {
        return usageMetricsRepository.findByUserId(userId)
                .doOnComplete(() -> {
                    log.debug("Retrieved usage metrics for user: {}", userId);
                    meterRegistry.counter("analytics.usage.retrieved").increment();
                });
    }

    // Performance Metrics
    public Mono<PerformanceMetrics> recordPerformanceMetrics(PerformanceMetrics metrics) {
        return performanceMetricsRepository.save(metrics)
                .doOnSuccess(saved -> {
                    log.info("Recorded performance metrics for model: {}, type: {}", 
                            saved.getModelId(), saved.getType());
                    meterRegistry.counter("analytics.performance.recorded").increment();
                });
    }

    public Flux<PerformanceMetrics> getModelPerformance(String modelId) {
        return performanceMetricsRepository.findByModelId(modelId)
                .doOnComplete(() -> {
                    log.debug("Retrieved performance metrics for model: {}", modelId);
                    meterRegistry.counter("analytics.performance.retrieved").increment();
                });
    }

    // Analytics Reports
    public Mono<AnalyticsReport> generateReport(AnalyticsReport report) {
        report.setStatus(AnalyticsReport.ReportStatus.GENERATING);
        return analyticsReportRepository.save(report)
                .flatMap(this::processReport)
                .doOnSuccess(saved -> {
                    log.info("Generated report: {}, type: {}", saved.getId(), saved.getType());
                    meterRegistry.counter("analytics.report.generated").increment();
                });
    }

    private Mono<AnalyticsReport> processReport(AnalyticsReport report) {
        return switch (report.getType()) {
            case USAGE_SUMMARY -> processUsageSummaryReport(report);
            case PERFORMANCE_ANALYSIS -> processPerformanceAnalysisReport(report);
            case ERROR_ANALYSIS -> processErrorAnalysisReport(report);
            case COST_ANALYSIS -> processCostAnalysisReport(report);
            case USER_BEHAVIOR -> processUserBehaviorReport(report);
            case MODEL_COMPARISON -> processModelComparisonReport(report);
            case CUSTOM -> processCustomReport(report);
        };
    }

    private Mono<AnalyticsReport> processUsageSummaryReport(AnalyticsReport report) {
        return usageMetricsRepository.findByTimeRange(report.getReportStartTime(), report.getReportEndTime())
                .collectList()
                .map(metrics -> {
                    Map<String, Object> data = aggregateUsageData(metrics);
                    report.setData(data);
                    report.setStatus(AnalyticsReport.ReportStatus.COMPLETED);
                    return report;
                })
                .flatMap(analyticsReportRepository::save);
    }

    private Mono<AnalyticsReport> processPerformanceAnalysisReport(AnalyticsReport report) {
        return performanceMetricsRepository.findByTimeRange(report.getReportStartTime(), report.getReportEndTime())
                .collectList()
                .map(metrics -> {
                    Map<String, Object> data = aggregatePerformanceData(metrics);
                    report.setData(data);
                    report.setStatus(AnalyticsReport.ReportStatus.COMPLETED);
                    return report;
                })
                .flatMap(analyticsReportRepository::save);
    }

    // Helper methods for data aggregation
    private Map<String, Object> aggregateUsageData(java.util.List<UsageMetrics> metrics) {
        // Implement usage data aggregation logic
        return Map.of("metrics", metrics);
    }

    private Map<String, Object> aggregatePerformanceData(java.util.List<PerformanceMetrics> metrics) {
        // Implement performance data aggregation logic
        return Map.of("metrics", metrics);
    }

    // Other report processing methods would be implemented similarly
    private Mono<AnalyticsReport> processErrorAnalysisReport(AnalyticsReport report) {
        // Implement error analysis report processing
        return Mono.just(report);
    }

    private Mono<AnalyticsReport> processCostAnalysisReport(AnalyticsReport report) {
        // Implement cost analysis report processing
        return Mono.just(report);
    }

    private Mono<AnalyticsReport> processUserBehaviorReport(AnalyticsReport report) {
        // Implement user behavior report processing
        return Mono.just(report);
    }

    private Mono<AnalyticsReport> processModelComparisonReport(AnalyticsReport report) {
        // Implement model comparison report processing
        return Mono.just(report);
    }

    private Mono<AnalyticsReport> processCustomReport(AnalyticsReport report) {
        // Implement custom report processing
        return Mono.just(report);
    }

    public Flux<AnalyticsReport> getReportsByType(AnalyticsReport.ReportType type) {
        return analyticsReportRepository.findByType(type)
                .doOnComplete(() -> {
                    log.debug("Retrieved reports by type: {}", type);
                    meterRegistry.counter("analytics.report.retrieved").increment();
                });
    }

    public Flux<AnalyticsReport> getReportsByUser(String userId) {
        return analyticsReportRepository.findByCreatedBy(userId)
                .doOnComplete(() -> {
                    log.debug("Retrieved reports for user: {}", userId);
                    meterRegistry.counter("analytics.report.retrieved").increment();
                });
    }

    private void publishAnalyticsEvent(String key, Object event) {
        kafkaTemplate.send(analyticsEventsTopic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish analytics event", ex);
                } else {
                    log.debug("Analytics event published successfully");
                }
            });
    }
}