package com.jirapat.prpo.api.dto.request;

import com.jirapat.prpo.api.entity.PurchaseOrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePurchaseOrderStatusRequest {

    @NotNull(message = "Status is required")
    private PurchaseOrderStatus status;

    private java.time.LocalDate actualDeliveryDate;

    private String remark;

}
