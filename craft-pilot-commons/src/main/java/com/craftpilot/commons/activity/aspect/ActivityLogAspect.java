package com.craftpilot.commons.activity.aspect;

import com.craftpilot.commons.activity.annotation.LogActivity;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Slf4j
public class ActivityLogAspect {
    
    private final ActivityLogger activityLogger;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    public ActivityLogAspect(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
    
    @Around("@annotation(logActivity)")
    public Object logActivityAround(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        Object result = joinPoint.proceed();
        
        if (result instanceof Mono<?>) {
            // Reaktif programlama ile uyumlu olarak düzenliyoruz
            return ((Mono<?>) result).doOnSuccess(value -> {
                if (value != null) {
                    try {
                        String userId = extractUserId(joinPoint, logActivity, value);
                        Map<String, Object> metadata = extractMetadata(joinPoint, logActivity, value);
                        
                        activityLogger.log(userId, logActivity.actionType(), metadata)
                            .subscribe();
                    } catch (Exception e) {
                        log.error("Error logging activity for reactive result", e);
                    }
                }
            }).doOnError(error -> {
                log.warn("Error in reactive operation, activity not logged: {}", error.getMessage());
            });
        } else {
            // Senkron operasyonlar için mevcut kod aynı kalabilir
            try {
                String userId = extractUserId(joinPoint, logActivity, result);
                Map<String, Object> metadata = extractMetadata(joinPoint, logActivity, result);
                
                activityLogger.log(userId, logActivity.actionType(), metadata)
                    .subscribe();
            } catch (Exception e) {
                log.error("Error logging activity", e);
            }
        }
        
        return result;
    }
    
    private String extractUserId(ProceedingJoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            String userIdParam = logActivity.userIdParam();
            
            if (userIdParam.isEmpty()) {
                return "system";
            }
            
            StandardEvaluationContext context = createEvaluationContext(joinPoint, result);
            Expression expression = parser.parseExpression(userIdParam);
            
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to extract user ID: {}", e.getMessage());
            return "unknown";
        }
    }
    
    private Map<String, Object> extractMetadata(ProceedingJoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            String metadataExp = logActivity.metadata();
            if (metadataExp.isEmpty()) {
                return new HashMap<>();
            }
            
            StandardEvaluationContext context = createEvaluationContext(joinPoint, result);
            
            // Doğrudan JSON formatında olduğu için parse etme
            if (metadataExp.startsWith("{") && metadataExp.endsWith("}")) {
                // JSON formatındaki string ifadeleri değerlendirme
                String[] keyValuePairs = metadataExp.substring(1, metadataExp.length() - 1).split(",");
                Map<String, Object> metadata = new HashMap<>();
                
                for (String pair : keyValuePairs) {
                    String[] parts = pair.trim().split(":");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        if (key.startsWith("\"") && key.endsWith("\"")) {
                            key = key.substring(1, key.length() - 1);
                        }
                        
                        String valueExpr = parts[1].trim();
                        try {
                            Expression expression = parser.parseExpression(valueExpr);
                            Object value = expression.getValue(context);
                            metadata.put(key, value);
                        } catch (Exception e) {
                            // Improve error logging with more detailed information
                            log.warn("Failed to evaluate expression '{}' for key {}: {}", 
                                    valueExpr, key, e.getMessage());
                            // Don't fail the entire metadata extraction, just skip this key
                            // and continue with the others
                        }
                    }
                }
                
                return metadata;
            } else {
                // Normal SpEL ifadesi değerlendirme
                Expression expression = parser.parseExpression(metadataExp);
                Object value = expression.getValue(context);
                
                if (value instanceof Map) {
                    return (Map<String, Object>) value;
                } else {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("value", value);
                    return metadata;
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract metadata: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    private StandardEvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Method argümanlarını context'e ekle
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (i < args.length) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
        }
        
        // Şimdi resulti context'e ekle
        context.setVariable("result", result);
        
        return context;
    }
}
