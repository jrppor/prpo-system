package com.jirapat.prpo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.response.PurchaseRequestItemResponse;
import com.jirapat.prpo.entity.PurchaseRequestItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface  PurchaseRequestItemMapper {

    PurchaseRequestItemResponse toPurchaseRequestItemResponse(PurchaseRequestItem purchaseRequestItem);

}
