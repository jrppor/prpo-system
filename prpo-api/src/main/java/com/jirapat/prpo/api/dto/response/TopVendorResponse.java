package com.jirapat.prpo.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TopVendorResponse {
    private UUID vendorId;
    private String vendorName;
    private long orderCount;
    private BigDecimal totalAmount;
}
