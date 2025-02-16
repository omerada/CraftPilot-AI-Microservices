package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.dto.UserPreferenceResponse;
import com.craftpilot.userservice.model.UserPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserPreferenceMapper {
    
    @Mapping(target = "createdAt", expression = "java(System.currentTimeMillis())")
    @Mapping(target = "updatedAt", expression = "java(System.currentTimeMillis())")
    @Mapping(target = "userId", ignore = true)
    UserPreference toEntity(UserPreferenceRequest request);

    UserPreferenceResponse toResponse(UserPreference preference);
}
