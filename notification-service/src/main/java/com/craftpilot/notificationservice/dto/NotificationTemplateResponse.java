package com.craftpilot.notificationservice.dto;

import com.craftpilot.notificationservice.model.NotificationTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationTemplateResponse {
    private String id;
    private String name;
    private String titleTemplate;
    private String contentTemplate;
    private Map<String, String> requiredVariables;
    private Map<String, Object> defaultValues;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static NotificationTemplateResponse fromEntity(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .titleTemplate(template.getTitleTemplate())
                .contentTemplate(template.getContentTemplate())
                .requiredVariables(template.getRequiredVariables())
                .defaultValues(template.getDefaultValues())
                .isActive(template.isActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
} 