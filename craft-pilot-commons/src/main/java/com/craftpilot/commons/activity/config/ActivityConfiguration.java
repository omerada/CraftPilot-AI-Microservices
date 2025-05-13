package com.craftpilot.commons.activity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "craftpilot.activity")
public class ActivityConfiguration {
    
    private boolean enabled = true;
    private String topic = "activity-events";
    private String errorHandling = "log-only";
    private String serviceNamePrefix = "";
    
}
