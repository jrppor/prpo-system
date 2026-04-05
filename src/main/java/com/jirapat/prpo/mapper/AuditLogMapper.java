package com.jirapat.prpo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.response.AuditLogResponse;
import com.jirapat.prpo.entity.AuditLog;
import com.jirapat.prpo.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    @Mapping(source = "performedBy", target = "performedBy", qualifiedByName = "userToFullName")
    AuditLogResponse toResponse(AuditLog auditLog);

    @Named("userToFullName")
    default String userToFullName(User user) {
        if (user == null) return null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
