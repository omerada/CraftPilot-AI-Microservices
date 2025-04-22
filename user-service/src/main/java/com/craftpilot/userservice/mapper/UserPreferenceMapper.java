package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.model.UserPreference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserPreferenceMapper {

    public UserPreference toEntity(UserPreferenceRequest dto) {
        if (dto == null) {
            return null;
        }

        return UserPreference.builder()
                .theme(dto.getTheme())
                .themeSchema(dto.getThemeSchema())
                .language(dto.getLanguage())
                .layout(dto.getLayout())
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
                .themeSchema(entity.getThemeSchema())
                .language(entity.getLanguage())
                .layout(entity.getLayout())
                .notifications(entity.getNotifications())
                .pushEnabled(entity.getPushEnabled())
                .aiModelFavorites(entity.getAiModelFavorites())
                .build();
    }
}
