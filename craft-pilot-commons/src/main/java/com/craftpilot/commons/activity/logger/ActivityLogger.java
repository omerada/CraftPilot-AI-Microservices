package com.craftpilot.commons.activity.logger;

import com.craftpilot.commons.activity.config.ActivityConfiguration;
import com.craftpilot.commons.activity.model.ActivityEvent;
import com.craftpilot.commons.activity.producer.ActivityProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ActivityLogger {
    private final ActivityProducer activityProducer;
    private final ActivityConfiguration config;
    
    public ActivityLogger(ActivityProducer activityProducer, ActivityConfiguration config) {
        this.activityProducer = activityProducer;
        this.config = config;
    }
    
    /**
     * Kullanıcı aktivitesi oluşturup gönderir
     */
    public Mono<Void> log(String userId, String actionType, Map<String, Object> metadata) {
        String finalActionType = getActionTypeWithPrefix(actionType);
        
        ActivityEvent event = ActivityEvent.builder()
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .actionType(finalActionType)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
                
        return activityProducer.sendEvent(event);
    }
    
    /**
     * Basitleştirilmiş loglama metodu
     */
    public Mono<Void> log(String userId, String actionType) {
        return log(userId, actionType, null);
    }
    
    /**
     * Opsiyonel olarak metadata ile birleştirilerek kullanım
     */
    public Mono<Void> log(String userId, String actionType, String key, Object value) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(key, value);
        return log(userId, actionType, metadata);
    }
    
    /**
     * Configuration'da tanımlanan servis önekini action type'a ekler
     */
    private String getActionTypeWithPrefix(String actionType) {
        if (StringUtils.hasText(config.getServiceNamePrefix()) && 
            !actionType.startsWith(config.getServiceNamePrefix())) {
            return config.getServiceNamePrefix() + "_" + actionType;
        }
        return actionType;
    }
}
