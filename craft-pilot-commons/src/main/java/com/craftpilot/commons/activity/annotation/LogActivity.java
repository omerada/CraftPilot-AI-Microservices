package com.craftpilot.commons.activity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodun çalışması sonucunda otomatik aktivite kaydı oluşturmak için kullanılır.
 * Aspect tarafından işlenir.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    /**
     * Aktivite tipi
     */
    String actionType();
    
    /**
     * Kullanıcı ID'si hangi parametre üzerinden alınacak
     * Parametre adı veya SpEL ifadesi olabilir
     * Örn: "userId" veya "#chatHistory.userId"
     */
    String userIdParam() default "userId";
    
    /**
     * Ek metadatalar için SpEL ifadesi
     * Örn: "{id: #result.id, title: #result.title}"
     */
    String metadata() default "";
}
