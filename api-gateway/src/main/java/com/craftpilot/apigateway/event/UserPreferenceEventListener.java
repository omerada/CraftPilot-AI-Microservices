package com.craftpilot.apigateway.event;

import com.craftpilot.apigateway.cache.UserPreferenceCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferenceEventListener {

    private final UserPreferenceCache userPreferenceCache;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "${kafka.topics.user-preferences}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserPreferenceEvent(ConsumerRecord<String, String> record) {
        try {
            String key = record.key();
            String value = record.value();
            
            log.debug("Received user preference event: {}", key);
            
            Map<String, Object> eventData = objectMapper.readValue(value, Map.class);
            String userId = (String) eventData.get("userId");
            String eventType = (String) eventData.get("eventType");
            
            if (userId != null && !userId.isEmpty()) {
                if ("PREFERENCES_UPDATED".equals(eventType)) {
                    // Önbelleği güncelle
                    userPreferenceCache.invalidate(userId);
                    userPreferenceCache.refreshCache(userId);
                    log.info("Invalidated and refreshed cache for user: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing user preference event: {}", e.getMessage(), e);
        }
    }
}
