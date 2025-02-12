package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.dto.UserPreferenceDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserPreferenceMapper {
    
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "theme", source = "theme")
    @Mapping(target = "language", source = "language")
    @Mapping(target = "notifications", source = "notifications")
    @Mapping(target = "pushEnabled", source = "pushEnabled")
    UserPreferenceDto toDto(UserPreference preference);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "theme", source = "theme")
    @Mapping(target = "language", source = "language")
    @Mapping(target = "notifications", source = "notifications")
    @Mapping(target = "pushEnabled", source = "pushEnabled")
    UserPreference toEntity(UserPreferenceDto dto);
}
