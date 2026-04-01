package com.jirapat.prpo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.jirapat.prpo.entity.PurchaseOrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseOrderSummaryResponse {
    private UUID id;
    private String poNumber;
    private String vendorName;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
}
