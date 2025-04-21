package com.craftpilot.activitylogservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEvent {
    
    @NotBlank(message = "User ID cannot be empty")
    private String userId;
    
    @NotNull(message = "Timestamp cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Action type cannot be empty")
    private String actionType;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() && 
               timestamp != null && 
               actionType != null && !actionType.trim().isEmpty();
    }
}
