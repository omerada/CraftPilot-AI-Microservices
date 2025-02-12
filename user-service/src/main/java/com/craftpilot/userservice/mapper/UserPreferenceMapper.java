package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserPreferenceResponse;
import com.craftpilot.userservice.model.UserPreference;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceMapper {
    
    public UserPreferenceResponse toResponse(UserPreference preference) {
        return UserPreferenceResponse.builder()
                .userId(preference.getUserId())
                .theme(preference.getTheme())
                .language(preference.getLanguage())
                .notifications(preference.isNotifications())
                .pushEnabled(preference.isPushEnabled())
                .build();
    }
}
