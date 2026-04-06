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

import com.jirapat.prpo.dto.response.PurchaseRequestItemResponse;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.entity.PurchaseRequestStatus;
import com.jirapat.prpo.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestPdfExportService Tests")
class PurchaseRequestPdfExportServiceTest {

    @Mock
    private PurchaseRequestService purchaseRequestService;

    @InjectMocks
    private PurchaseRequestPdfExportService pdfExportService;

    @Test
    @DisplayName("Should generate PDF with items")
    void shouldGeneratePdfWithItems() {
        UUID id = UUID.randomUUID();
        PurchaseRequestResponse pr = buildPurchaseRequestResponse(id);

        when(purchaseRequestService.getPurchaseRequestById(id)).thenReturn(pr);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        // PDF files start with %PDF
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should generate PDF with empty items list")
    void shouldGeneratePdfWithEmptyItems() {
        UUID id = UUID.randomUUID();
        PurchaseRequestResponse pr = PurchaseRequestResponse.builder()
                .id(id)
                .prNumber("PR-20250101-001")
                .title("Empty PR")
                .status(PurchaseRequestStatus.DRAFT)
                .requester("Test User")
                .totalAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();

        when(purchaseRequestService.getPurchaseRequestById(id)).thenReturn(pr);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate PDF with null optional fields")
    void shouldGeneratePdfWithNullFields() {
        UUID id = UUID.randomUUID();
        PurchaseRequestResponse pr = PurchaseRequestResponse.builder()
                .id(id)
                .prNumber("PR-20250101-002")
                .title("Minimal PR")
                .status(PurchaseRequestStatus.SUBMITTED)
                .requester(null)
                .department(null)
                .justification(null)
                .requiredDate(null)
                .totalAmount(null)
                .createdAt(null)
                .items(List.of())
                .build();

        when(purchaseRequestService.getPurchaseRequestById(id)).thenReturn(pr);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should propagate ResourceNotFoundException")
    void shouldPropagateResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(purchaseRequestService.getPurchaseRequestById(id))
                .thenThrow(new ResourceNotFoundException("PurchaseRequest", "id", id));

        assertThatThrownBy(() -> pdfExportService.exportToPdf(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should generate PDF with multiple items")
    void shouldGeneratePdfWithMultipleItems() {
        UUID id = UUID.randomUUID();
        PurchaseRequestResponse pr = PurchaseRequestResponse.builder()
                .id(id)
                .prNumber("PR-20250101-003")
                .title("Multi-item PR")
                .status(PurchaseRequestStatus.APPROVED)
                .requester("John Doe")
                .department("IT")
                .totalAmount(new BigDecimal("15000.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        PurchaseRequestItemResponse.builder()
                                .itemNumber(1)
                                .description("Laptop")
                                .quantity(new BigDecimal("2"))
                                .unit("pcs")
                                .estimatedPrice(new BigDecimal("5000.00"))
                                .totalPrice(new BigDecimal("10000.00"))
                                .build(),
                        PurchaseRequestItemResponse.builder()
                                .itemNumber(2)
                                .description("Mouse")
                                .quantity(new BigDecimal("5"))
                                .unit("pcs")
                                .estimatedPrice(new BigDecimal("1000.00"))
                                .totalPrice(new BigDecimal("5000.00"))
                                .build()
                ))
                .build();

        when(purchaseRequestService.getPurchaseRequestById(id)).thenReturn(pr);

        byte[] result = pdfExportService.exportToPdf(id);

        assertThat(result).isNotNull();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    private PurchaseRequestResponse buildPurchaseRequestResponse(UUID id) {
        return PurchaseRequestResponse.builder()
                .id(id)
                .prNumber("PR-20250101-001")
                .title("Office Supplies")
                .justification("Monthly restock")
                .totalAmount(new BigDecimal("25000.00"))
                .status(PurchaseRequestStatus.APPROVED)
                .department("IT")
                .requester("John Doe")
                .requiredDate(LocalDate.of(2025, 2, 1))
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 30))
                .items(List.of(
                        PurchaseRequestItemResponse.builder()
                                .itemNumber(1)
                                .description("A4 Paper")
                                .quantity(new BigDecimal("100"))
                                .unit("ream")
                                .estimatedPrice(new BigDecimal("150.00"))
                                .totalPrice(new BigDecimal("15000.00"))
                                .build(),
                        PurchaseRequestItemResponse.builder()
                                .itemNumber(2)
                                .description("Printer Ink")
                                .quantity(new BigDecimal("10"))
                                .unit("cartridge")
                                .estimatedPrice(new BigDecimal("1000.00"))
                                .totalPrice(new BigDecimal("10000.00"))
                                .build()
                ))
                .build();
    }
}
