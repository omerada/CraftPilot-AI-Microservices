package com.craftpilot.contentservice.model.dto;

import com.craftpilot.contentservice.model.ContentStatus;
import com.craftpilot.contentservice.model.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String content;
    private ContentType type;
    private List<String> tags;
    private Map<String, Object> metadata;
    private ContentStatus status;
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
} 