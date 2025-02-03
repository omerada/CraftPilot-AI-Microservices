package com.craftpilot.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Content {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String content;
    private ContentType type;
    private ContentStatus status;
    private List<String> tags;
    private Map<String, Object> metadata;
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
} 