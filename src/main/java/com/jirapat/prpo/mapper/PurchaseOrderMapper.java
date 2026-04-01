package com.jirapat.prpo.mapper;

import com.jirapat.prpo.dto.request.CreatePurchaseOrderRequest;
import com.jirapat.prpo.dto.request.PurchaseOrderItemRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseOrderRequest;
import com.jirapat.prpo.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.entity.PurchaseOrder;
import com.jirapat.prpo.entity.PurchaseOrderItem;
import com.jirapat.prpo.entity.PurchaseRequest;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.entity.Vendor;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseOrderMapper {

    @Mapping(source = "purchaseRequest", target = "purchaseRequestNumber", qualifiedByName = "prToNumber")
    @Mapping(source = "vendor", target = "vendorName", qualifiedByName = "vendorToName")
    @Mapping(source = "createdBy", target = "createdByName", qualifiedByName = "userToFullName")
    PurchaseOrderResponse toResponse(PurchaseOrder purchaseOrder);

    @Mapping(source = "purchaseRequest", target = "purchaseRequestNumber", qualifiedByName = "prToNumber")
    @Mapping(source = "vendor", target = "vendorName", qualifiedByName = "vendorToName")
    @Mapping(source = "createdBy", target = "createdByName", qualifiedByName = "userToFullName")
    @Mapping(target = "items", ignore = true)
    PurchaseOrderResponse toListResponse(PurchaseOrder purchaseOrder);


    PurchaseOrderResponse.ItemResponse toItemResponse(PurchaseOrderItem item);

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
