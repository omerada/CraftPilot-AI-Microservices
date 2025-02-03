package com.craftpilot.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {
    
    @NotBlank(message = "Template name is required")
    private String name;
    
    @NotBlank(message = "Title template is required")
    private String titleTemplate;
    
    @NotBlank(message = "Content template is required")
    private String contentTemplate;
    
    @NotEmpty(message = "Required variables must be specified")
    private Map<String, String> requiredVariables;
    
    private Map<String, Object> defaultValues;
    
    @Builder.Default
    private boolean isActive = true;
} 