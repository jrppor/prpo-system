package com.jirapat.prpo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.dto.response.PurchaseOrderItemResponse;
import com.jirapat.prpo.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.entity.PurchaseOrderStatus;
import com.jirapat.prpo.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderPdfExportService Tests")
class PurchaseOrderPdfExportServiceTest {

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderPdfExportService pdfExportService;

    @Test
    @DisplayName("Should generate PDF with items")
    void shouldGeneratePdfWithItems() {
        UUID id = UUID.randomUUID();
        PurchaseOrderResponse po = buildPurchaseOrderResponse(id);

        when(purchaseOrderService.getPurchaseOrderById(id)).thenReturn(po);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should generate PDF with empty items list")
    void shouldGeneratePdfWithEmptyItems() {
        UUID id = UUID.randomUUID();
        PurchaseOrderResponse po = PurchaseOrderResponse.builder()
                .id(id)
                .poNumber("PO-20250101-001")
                .status(PurchaseOrderStatus.DRAFT)
                .vendorName("Test Vendor")
                .totalAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();

        when(purchaseOrderService.getPurchaseOrderById(id)).thenReturn(po);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate PDF with null optional fields")
    void shouldGeneratePdfWithNullFields() {
        UUID id = UUID.randomUUID();
        PurchaseOrderResponse po = PurchaseOrderResponse.builder()
                .id(id)
                .poNumber("PO-20250101-002")
                .status(PurchaseOrderStatus.SENT)
                .vendorName(null)
                .createdByName(null)
                .purchaseRequestNumber(null)
                .orderDate(null)
                .expectedDeliveryDate(null)
                .remark(null)
                .totalAmount(null)
                .createdAt(null)
                .items(List.of())
                .build();

        when(purchaseOrderService.getPurchaseOrderById(id)).thenReturn(po);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should propagate ResourceNotFoundException")
    void shouldPropagateResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(purchaseOrderService.getPurchaseOrderById(id))
                .thenThrow(new ResourceNotFoundException("PurchaseOrder", "id", id));

        assertThatThrownBy(() -> pdfExportService.exportToPdf(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should generate PDF with multiple items")
    void shouldGeneratePdfWithMultipleItems() {
        UUID id = UUID.randomUUID();
        PurchaseOrderResponse po = PurchaseOrderResponse.builder()
                .id(id)
                .poNumber("PO-20250101-003")
                .purchaseRequestNumber("PR-20250101-001")
                .vendorName("ABC Supplies Co.")
                .createdByName("Jane Smith")
                .status(PurchaseOrderStatus.SENT)
                .totalAmount(new BigDecimal("35000.00"))
                .orderDate(LocalDate.of(2025, 1, 20))
                .expectedDeliveryDate(LocalDate.of(2025, 2, 5))
                .remark("Urgent delivery")
                .createdAt(LocalDateTime.of(2025, 1, 20, 14, 0))
                .items(List.of(
                        PurchaseOrderItemResponse.builder()
                                .itemNumber(1)
                                .description("Laptop Dell XPS")
                                .quantity(new BigDecimal("2"))
                                .unit("pcs")
                                .unitPrice(new BigDecimal("15000.00"))
                                .totalPrice(new BigDecimal("30000.00"))
                                .build(),
                        PurchaseOrderItemResponse.builder()
                                .itemNumber(2)
                                .description("Wireless Mouse")
                                .quantity(new BigDecimal("5"))
                                .unit("pcs")
                                .unitPrice(new BigDecimal("1000.00"))
                                .totalPrice(new BigDecimal("5000.00"))
                                .build()
                ))
                .build();

        when(purchaseOrderService.getPurchaseOrderById(id)).thenReturn(po);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    private PurchaseOrderResponse buildPurchaseOrderResponse(UUID id) {
        return PurchaseOrderResponse.builder()
                .id(id)
                .poNumber("PO-20250101-001")
                .purchaseRequestNumber("PR-20250101-001")
                .vendorName("ABC Supplies Co.")
                .createdByName("Jane Smith")
                .status(PurchaseOrderStatus.SENT)
                .totalAmount(new BigDecimal("25000.00"))
                .orderDate(LocalDate.of(2025, 1, 20))
                .expectedDeliveryDate(LocalDate.of(2025, 2, 1))
                .remark("Standard delivery")
                .createdAt(LocalDateTime.of(2025, 1, 20, 10, 0))
                .items(List.of(
                        PurchaseOrderItemResponse.builder()
                                .itemNumber(1)
                                .description("A4 Paper")
                                .quantity(new BigDecimal("100"))
                                .unit("ream")
                                .unitPrice(new BigDecimal("150.00"))
                                .totalPrice(new BigDecimal("15000.00"))
                                .build(),
                        PurchaseOrderItemResponse.builder()
                                .itemNumber(2)
                                .description("Printer Ink")
                                .quantity(new BigDecimal("10"))
                                .unit("cartridge")
                                .unitPrice(new BigDecimal("1000.00"))
                                .totalPrice(new BigDecimal("10000.00"))
                                .build()
                ))
                .build();
    }
}
