package com.jirapat.prpo.api.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MonthlySpendingResponse {
    private int year;
    private int month;
    private BigDecimal totalAmount;
    private long orderCount;
}
