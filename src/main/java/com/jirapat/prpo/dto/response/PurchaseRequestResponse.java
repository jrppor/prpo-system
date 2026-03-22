package com.jirapat.prpo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.jirapat.prpo.entity.PurchaseRequestStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseRequestResponse {
    private UUID id;
    private String prNumber;
    private String title;
    private String justification;
    private BigDecimal totalAmount;
    private PurchaseRequestStatus status;
    private String department;
    private String requester;
    private LocalDate requiredDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItemResponse> items;

    @Data
    @Builder
    public static class ItemResponse {
        private UUID id;
        private Integer itemNumber;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal estimatedPrice;
        private BigDecimal totalPrice;
        private String remark;
    }
}
