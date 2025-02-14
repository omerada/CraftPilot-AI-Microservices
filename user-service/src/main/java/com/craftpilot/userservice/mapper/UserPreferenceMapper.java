package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.dto.UserPreferenceResponse;
import com.craftpilot.userservice.model.UserPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserPreferenceMapper {
    
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserPreference toEntity(UserPreferenceRequest request);

    UserPreferenceResponse toResponse(UserPreference preference);
}
