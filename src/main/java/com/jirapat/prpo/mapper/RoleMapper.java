package com.jirapat.prpo.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.request.CreateRoleRequest;
import com.jirapat.prpo.dto.request.UpdateRoleDetailRequest;
import com.jirapat.prpo.dto.response.RoleResponse;
import com.jirapat.prpo.entity.Role;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    RoleResponse toRoleResponse(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Role toEntity(CreateRoleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(UpdateRoleDetailRequest request, @MappingTarget Role entity);
}
