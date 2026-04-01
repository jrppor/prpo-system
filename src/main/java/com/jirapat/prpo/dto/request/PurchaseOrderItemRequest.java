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
public class PurchaseOrderItemRequest {
    @NotBlank(message = "Description is required")
    private String description;

    private BigDecimal quantity;

    @Size(max = 50, message = "Unit must not exceed 50 charectors")
    private String unit;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private String remark;


}
