package com.jirapat.prpo.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.api.dto.request.CreatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.request.PurchaseOrderItemRequest;
import com.jirapat.prpo.api.dto.request.UpdatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.response.PurchaseOrderItemResponse;
import com.jirapat.prpo.api.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.api.dto.response.PurchaseOrderSummaryResponse;
import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderItem;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.entity.Vendor;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseOrderMapper {

    @Mapping(source = "purchaseRequest", target = "purchaseRequestNumber", qualifiedByName = "prToNumber")
    @Mapping(source = "vendor", target = "vendorName", qualifiedByName = "vendorToName")
    @Mapping(source = "createdBy", target = "createdByName", qualifiedByName = "userToFullName")
    PurchaseOrderResponse toResponse(PurchaseOrder purchaseOrder);

    @Mapping(source = "purchaseRequest", target = "purchaseRequestNumber", qualifiedByName = "prToNumber")
    @Mapping(source = "vendor", target = "vendorName", qualifiedByName = "vendorToName")
    @Mapping(source = "createdBy", target = "createdByName", qualifiedByName = "userToFullName")
    PurchaseOrderResponse toListResponse(PurchaseOrder purchaseOrder);

    @Mapping(source = "vendor", target = "vendorName", qualifiedByName = "vendorToName")
    PurchaseOrderSummaryResponse toSummaryResponse(PurchaseOrder purchaseOrder);

    PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "poNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    PurchaseOrder toEntity(CreatePurchaseOrderRequest request);

    @Mapping(target = "id", ignore = true)
    PurchaseOrderItem toItemEntity(PurchaseOrderItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PurchaseOrder updateEntity(UpdatePurchaseOrderRequest request, @MappingTarget PurchaseOrder purchaseOrder);

    @Named("prToNumber")
    default String prToNumber(PurchaseRequest purchaseRequest) {
        if (purchaseRequest == null) return null;
        return purchaseRequest.getPrNumber();
    }

    @Named("vendorToName")
    default String vendorToName(Vendor vendor) {
        if (vendor == null) return null;
        return vendor.getName();
    }

    @Named("userToFullName")
    default String userToFullName(User user) {
        if (user == null) return null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
