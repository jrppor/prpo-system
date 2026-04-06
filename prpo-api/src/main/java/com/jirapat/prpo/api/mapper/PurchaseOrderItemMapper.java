package com.jirapat.prpo.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.api.dto.response.PurchaseOrderItemResponse;
import com.jirapat.prpo.api.entity.PurchaseOrderItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseOrderItemMapper {

    PurchaseOrderItemResponse toPurchaseOrderItemResponse(PurchaseOrderItem purchaseOrderItem);

}
