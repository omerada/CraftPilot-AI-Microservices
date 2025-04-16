package com.craftpilot.lighthouseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {
    private String jobId;
    private boolean complete;
    private String status;
    private String error;
    private Map<String, Object> data;
    private Long timestamp;
    
    // Debug ve izleme amaçlı yardımcı alan
    private String debugInfo;
}
