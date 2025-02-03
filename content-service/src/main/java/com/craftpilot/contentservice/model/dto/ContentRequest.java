package com.craftpilot.contentservice.model.dto;

import com.craftpilot.contentservice.model.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {
    private String userId;
    private String title;
    private String description;
    private String content;
    private ContentType type;
    private List<String> tags;
    private Map<String, Object> metadata;
} 