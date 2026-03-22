package com.jirapat.prpo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestItemRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    private BigDecimal estimatedPrice;

    @Size(max = 500, message = "Remark must not exceed 500 characters")
    private String remark;
}
