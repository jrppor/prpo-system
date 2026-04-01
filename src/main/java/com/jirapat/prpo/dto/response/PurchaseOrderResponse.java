package com.jirapat.prpo.dto.response;

import com.jirapat.prpo.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PurchaseOrderResponse {

    private UUID id;
    private String poNumber;
    private String purchaseRequestNumber;
    private String vendorName;
    private String createdByName;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String remark;
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
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String remark;
    }
}
