// Model sınıfını MongoDB için güncelleyelim
package com.craftpilot.llmservice.model;

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
@Document(collection = "performance_analyses")
public class PerformanceAnalysis {
    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String userId;

    @Indexed
    private String modelId;

    private String requestId;

    @Indexed
    private LocalDateTime timestamp;

    private Long responseTimeMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    private Map<String, Object> metadata;
    private Map<String, Object> metrics;
}
