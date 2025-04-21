package com.craftpilot.commons.activity.aspect;

import com.craftpilot.commons.activity.annotation.LogActivity;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Slf4j
public class ActivityLogAspect {
    private final ActivityLogger activityLogger;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    public ActivityLogAspect(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
    
    @Around("@annotation(com.craftpilot.commons.activity.annotation.LogActivity)")
    public Object logActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        // Metot ve annotation bilgilerini al
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogActivity annotation = method.getAnnotation(LogActivity.class);
        
        // Parametre isimlerini ve değerlerini al
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        String[] paramNames = codeSignature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        
        // Metodu çalıştır ve sonucu al
        Object result = joinPoint.proceed();
        
        try {
            // SpEL context'i oluştur ve parametre değerlerini ekle
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], paramValues[i]);
            }
            
            // Sonuç değerini de ekle
            if (result != null) {
                context.setVariable("result", result);
            }
            
            // UserID'yi çıkar
            String userId = extractUserId(annotation.userIdParam(), context);
            
            // Metadata oluştur
            Map<String, Object> metadata = createMetadata(annotation.metadata(), context);
            
            // Aktivite logu oluştur (ve hataları yut)
            activityLogger.log(userId, annotation.actionType(), metadata)
                .doOnError(e -> log.error("Failed to log activity: {}", e.getMessage()))
                .subscribe();
        } catch (Exception e) {
            // Aktivite loglaması başarısız olsa bile ana uygulama çalışmaya devam etmeli
            log.error("Activity logging failed: {}", e.getMessage());
        }
        
        return result;
    }
    
    private String extractUserId(String userIdParam, StandardEvaluationContext context) {
        if (userIdParam == null || userIdParam.isEmpty()) {
            return "unknown";
        }
        
        try {
            if (userIdParam.startsWith("#")) {
                // SpEL ifadesi
                Expression expression = expressionParser.parseExpression(userIdParam);
                Object value = expression.getValue(context);
                return value != null ? value.toString() : "unknown";
            } else {
                // Parametre adı
                Object value = context.lookupVariable(userIdParam);
                return value != null ? value.toString() : "unknown";
            }
        } catch (Exception e) {
            log.warn("Failed to extract user ID: {}", e.getMessage());
            return "unknown";
        }
    }
    
    private Map<String, Object> createMetadata(String metadataExpression, StandardEvaluationContext context) {
        if (metadataExpression == null || metadataExpression.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Expression expression = expressionParser.parseExpression(metadataExpression);
            Object value = expression.getValue(context);
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) value;
                return result;
            } else {
                log.warn("Metadata expression did not evaluate to a map: {}", metadataExpression);
                return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("Failed to evaluate metadata expression: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
