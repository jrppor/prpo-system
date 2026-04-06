package com.jirapat.prpo.api.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePurchaseOrderRequest {

    @NotNull(message = "vendor is required")
    private UUID vendorId;

    private UUID purchaseRequestId;

    private LocalDate expectedDeliveryDate;

    private String remark;

    private List<PurchaseOrderItemRequest> items;
}
