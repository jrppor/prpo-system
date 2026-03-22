package com.jirapat.prpo.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreatePurchaseRequestRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String justification;

    private BigDecimal totalAmount;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @NotNull(message = "Required date is required")
    private LocalDate requiredDate;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<PurchaseRequestItemRequest> items;
}
