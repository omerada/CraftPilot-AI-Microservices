package com.craftpilot.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {
    private String userId;
    private String language;
    private String timezone;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean smsEnabled;
    private Long quietHoursStart;
    private Long quietHoursEnd;
}