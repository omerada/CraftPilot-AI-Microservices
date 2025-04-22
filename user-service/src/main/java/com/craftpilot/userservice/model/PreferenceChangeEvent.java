package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceChangeEvent {
    private String userId;
    private long timestamp;
    private String preferenceType;
    private Map<String, Object> preferenceData;
}
