package com.jirapat.prpo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.dto.response.PurchaseOrderItemResponse;
import com.jirapat.prpo.entity.PurchaseOrderItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseOrderItemMapper {

    PurchaseOrderItemResponse toPurchaseOrderItemResponse(PurchaseOrderItem purchaseOrderItem);

}
