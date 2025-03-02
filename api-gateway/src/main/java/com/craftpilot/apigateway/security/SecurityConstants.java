package com.craftpilot.apigateway.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class SecurityConstants {
    
    private SecurityConstants() {
        // Utility sınıfını instance'laştırmayı önlemek için
    }

    // Public endpoints
    public static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
        "/actuator/**",
        "/public/**",
        "/auth/**",
        "/login",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/webjars/**"
    ));

    // Admin only paths
    public static final Set<String> ADMIN_PATHS = new HashSet<>(Arrays.asList(
        "/admin/**",
        "/management/**"
    ));

    public static boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    public static boolean isAdminPath(String path) {
        return ADMIN_PATHS.stream().anyMatch(path::startsWith);
    }
}
