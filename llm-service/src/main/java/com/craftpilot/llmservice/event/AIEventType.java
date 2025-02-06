package com.craftpilot.llmservice.event;

public enum AIEventType {
    STARTED,    // AI isteği başladığında
    COMPLETED,  // AI isteği başarıyla tamamlandığında
    FAILED,     // AI isteği başarısız olduğunda
    RATE_LIMITED, // Rate limit aşıldığında
    CACHED      // Önbellekten yanıt döndüğünde
} 