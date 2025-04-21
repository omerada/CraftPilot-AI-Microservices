package com.craftpilot.commons.activity.model;

public final class ActivityEventTypes {
    // Genel aktivite tipleri
    public static final String USER_LOGIN = "USER_LOGIN";
    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String USER_REGISTER = "USER_REGISTER";
    public static final String USER_UPDATE_PROFILE = "USER_UPDATE_PROFILE";
    
    // LLM servisi için aktivite tipleri
    public static final String CHAT_HISTORY_CREATE = "CHAT_HISTORY_CREATE";
    public static final String CHAT_HISTORY_UPDATE = "CHAT_HISTORY_UPDATE";
    public static final String CHAT_HISTORY_DELETE = "CHAT_HISTORY_DELETE";
    public static final String CHAT_HISTORY_ARCHIVE = "CHAT_HISTORY_ARCHIVE";
    public static final String CONVERSATION_CREATE = "CONVERSATION_CREATE";
    public static final String TITLE_UPDATE = "TITLE_UPDATE";
    
    // Admin servisi için aktivite tipleri
    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String ADMIN_ACTION = "ADMIN_ACTION";
    
    // Analytics servisi için aktivite tipleri
    public static final String ANALYTICS_REPORT_GENERATE = "ANALYTICS_REPORT_GENERATE";
    public static final String ANALYTICS_REPORT_VIEW = "ANALYTICS_REPORT_VIEW";
    
    // Özel bir aktivite tipi oluşturmak için yardımcı metot
    public static String custom(String serviceName, String action) {
        return serviceName.toUpperCase() + "_" + action.toUpperCase();
    }
    
    private ActivityEventTypes() {
        // Constructor'ı private yaparak örnek oluşturulmasını engelliyoruz
    }
}
