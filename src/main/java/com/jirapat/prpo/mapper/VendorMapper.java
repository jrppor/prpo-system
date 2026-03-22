package com.jirapat.prpo.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.request.CreateVendorRequest;
import com.jirapat.prpo.dto.request.UpdateVendorRequest;
import com.jirapat.prpo.dto.response.VendorResponse;
import com.jirapat.prpo.entity.Vendor;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VendorMapper {

    VendorResponse toVendorResponse(Vendor vendor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Vendor toEntity(CreateVendorRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(UpdateVendorRequest request, @MappingTarget Vendor entity);
}
