package com.craftpilot.apigateway.security;

import org.springframework.stereotype.Component;

@Component
public class SecurityPathsMatcher {
    
    private static final String[] PUBLIC_PATHS = {
        "/actuator/**",
        "/public/**",
        "/auth/**",
        "/login",
        "/logout",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/health",
        "/info",
        "/favicon.ico",
        "/error"
    };
    
    private static final String[] ADMIN_PATHS = {
        "/admin/**",
        "/management/**"
    };
    
    public String[] getPublicPaths() {
        return PUBLIC_PATHS;
    }
    
    public String[] getAdminPaths() {
        return ADMIN_PATHS;
    }
    
    public boolean isPublicPath(String path) {
        for (String pattern : PUBLIC_PATHS) {
            if (matchesPattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isAdminPath(String path) {
        for (String pattern : ADMIN_PATHS) {
            if (matchesPattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }
}
