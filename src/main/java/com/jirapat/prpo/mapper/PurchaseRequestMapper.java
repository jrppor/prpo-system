package com.jirapat.prpo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.dto.request.PurchaseRequestItemRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseRequestRequest;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.entity.PurchaseRequest;
import com.jirapat.prpo.entity.PurchaseRequestItem;
import com.jirapat.prpo.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseRequestMapper {

    @Mapping(source = "requester", target = "requester", qualifiedByName = "userToFullName")
    PurchaseRequestResponse toPurchaseRequestResponse(PurchaseRequest purchaseRequest);

    @Mapping(source = "requester", target = "requester", qualifiedByName = "userToFullName")
    @Mapping(target = "items", ignore = true)
    PurchaseRequestResponse toListResponse(PurchaseRequest purchaseRequest);

    PurchaseRequestResponse.ItemResponse toItemResponse(PurchaseRequestItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "prNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "items", ignore = true)
    PurchaseRequest toEntity(CreatePurchaseRequestRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchaseRequest", ignore = true)
    @Mapping(target = "itemNumber", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PurchaseRequestItem toItemEntity(PurchaseRequestItemRequest request);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "prNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdatePurchaseRequestRequest request, @MappingTarget PurchaseRequest purchaseRequest);

    @Named("userToFullName")
    default String userToFullName(User user) {
        if (user == null) return null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
