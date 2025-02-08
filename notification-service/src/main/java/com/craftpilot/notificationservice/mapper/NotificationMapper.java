package com.craftpilot.notificationservice.mapper;

import com.craftpilot.notificationservice.model.EmailRequest;
import com.craftpilot.notificationservice.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    
    @Mapping(source = "recipient", target = "to")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "subject", target = "subject")
    EmailRequest toEmailRequest(Notification notification);
}
