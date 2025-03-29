package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.model.UserPreference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class UserPreferenceMapper {

    public UserPreference toEntity(UserPreferenceRequest dto) {
        if (dto == null) {
            return null;
        }

        return UserPreference.builder()
                .theme(dto.getTheme())
                .language(dto.getLanguage())
                .notifications(dto.getNotifications())
                .pushEnabled(dto.getPushEnabled())
                .aiModelFavorites(dto.getAiModelFavorites())
                .build();
    }

    public UserPreferenceRequest toDto(UserPreference entity) {
        if (entity == null) {
            return null;
        }

        return UserPreferenceRequest.builder()
                .theme(entity.getTheme())
                .language(entity.getLanguage())
                .notifications(entity.getNotifications())
                .pushEnabled(entity.getPushEnabled())
                .aiModelFavorites(entity.getAiModelFavorites())
                .build();
    }
}
