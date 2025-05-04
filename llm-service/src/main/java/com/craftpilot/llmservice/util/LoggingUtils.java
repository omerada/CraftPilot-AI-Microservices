package com.craftpilot.llmservice.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

@Slf4j
public class LoggingUtils {
    
    public static void setRequestContext(String requestId, String userId) {
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        if (userId != null) {
            MDC.put("userId", userId);
        }
    }
    
    public static void clearRequestContext() {
        MDC.clear();
    }
    
    public static String truncateForLogging(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
