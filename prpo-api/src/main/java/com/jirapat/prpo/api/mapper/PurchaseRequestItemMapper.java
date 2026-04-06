package com.jirapat.prpo.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.api.dto.response.PurchaseRequestItemResponse;
import com.jirapat.prpo.api.entity.PurchaseRequestItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface  PurchaseRequestItemMapper {

    PurchaseRequestItemResponse toPurchaseRequestItemResponse(PurchaseRequestItem purchaseRequestItem);

}
