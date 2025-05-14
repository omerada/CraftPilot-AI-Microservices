package com.craftpilot.userservice.mapper;

import com.craftpilot.userservice.dto.UserDTO;
import com.craftpilot.userservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(source = "email", target = "email")
    @Mapping(source = "name", target = "displayName")
    @Mapping(source = "externalId", target = "uid")
    @Mapping(source = "updatedAt", target = "lastUpdatedAt")
    @Mapping(target = "lastLoginAt", expression = "java(java.time.LocalDateTime.now())")
    UserDTO toDto(User user);
    
    @Mapping(source = "email", target = "email")
    @Mapping(source = "displayName", target = "name")
    @Mapping(source = "uid", target = "externalId")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "roles", expression = "java(java.util.Map.of(\"user\", true))")
    @Mapping(target = "favoriteModelIds", expression = "java(new java.util.ArrayList<>())")
    User toEntity(UserDTO userDTO);
}
