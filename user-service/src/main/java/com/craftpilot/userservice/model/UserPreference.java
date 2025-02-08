package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String theme;
    private String language;
    private boolean emailNotifications;
    private boolean pushNotifications;
    // Add other preference fields as needed
}