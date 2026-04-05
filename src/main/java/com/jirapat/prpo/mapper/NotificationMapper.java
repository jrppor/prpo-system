package com.jirapat.prpo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.response.NotificationResponse;
import com.jirapat.prpo.entity.Notification;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
