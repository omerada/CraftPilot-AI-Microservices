package com.craftpilot.apigateway.security;

import java.util.List;

public class SecurityConstants {
    public static final List<String> PUBLIC_PATHS = List.of(
        "/auth/**",
        "/fallback/**",
        "/actuator/**",
        "/auth/register",
        "/auth/login",
        "/auth/verify-email",
        "/auth/reset-password",
        "/subscription-plans/public/**",
        "/docs/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/favicon.ico"
    );

    public static boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> {
                    if (pattern.endsWith("/**")) {
                        String basePattern = pattern.substring(0, pattern.length() - 3);
                        return path.startsWith(basePattern);
                    }
                    return path.equals(pattern);
                });
    }
}
