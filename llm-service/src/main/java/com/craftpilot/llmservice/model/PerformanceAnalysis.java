// Model sınıfını MongoDB için güncelleyelim
package com.craftpilot.llmservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "performance_analyses")
@CompoundIndexes({
        @CompoundIndex(name = "userId_modelId_idx", def = "{'userId': 1, 'modelId': 1}"),
        @CompoundIndex(name = "timestamp_sessionId_idx", def = "{'timestamp': -1, 'sessionId': 1}")
})
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

    @Field("response_time_ms")
    private Long responseTimeMs;

    @Field("prompt_tokens")
    private Integer promptTokens;

    @Field("completion_tokens")
    private Integer completionTokens;

    @Field("total_tokens")
    private Integer totalTokens;

    private Map<String, Object> metadata;
    private Map<String, Object> metrics;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String status;
    private String errorMessage;
    private String url;
}
