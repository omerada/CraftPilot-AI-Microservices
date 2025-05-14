package com.craftpilot.llmservice.model.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "performance_analysis_responses")
public class PerformanceAnalysisResponse {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    private String url;

    @Indexed
    private String modelId;

    private String jobId;

    @Indexed
    private LocalDateTime timestamp;

    private Long processingTimeMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;

    private Double responseQualityScore;
    private Map<String, Object> metrics;
    private Map<String, Object> metadata;

    private String requestType;
    private String status;
    private String errorMessage;
    private String message;

    // Performance score
    private Double performance;
}
