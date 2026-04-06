package com.jirapat.prpo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.dto.response.DashboardSummaryResponse;
import com.jirapat.prpo.dto.response.MonthlySpendingResponse;
import com.jirapat.prpo.dto.response.TopVendorResponse;
import com.jirapat.prpo.entity.PurchaseOrderStatus;
import com.jirapat.prpo.entity.PurchaseRequestStatus;
import com.jirapat.prpo.repository.PurchaseOrderRepository;
import com.jirapat.prpo.repository.PurchaseRequestRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Nested
    @DisplayName("getSummary()")
    class GetSummaryTests {

        @Test
        @DisplayName("should return summary with all counts")
        void getSummary_ReturnsCounts() {
            when(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.DRAFT)).thenReturn(5L);
            when(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.SUBMITTED)).thenReturn(3L);
            when(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.MANAGER_APPROVED)).thenReturn(2L);
            when(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.APPROVED)).thenReturn(10L);
            when(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.REJECTED)).thenReturn(1L);
            when(purchaseRequestRepository.count()).thenReturn(21L);

            when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.DRAFT)).thenReturn(4L);
            when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.SENT)).thenReturn(6L);
            when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.RECEIVED)).thenReturn(2L);
            when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.COMPLETED)).thenReturn(8L);
            when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.CANCELLED)).thenReturn(1L);
            when(purchaseOrderRepository.count()).thenReturn(21L);

            when(purchaseOrderRepository.sumTotalAmountByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(BigDecimal.valueOf(250000));

            DashboardSummaryResponse result = dashboardService.getSummary();

            assertThat(result.getPurchaseRequests().getDraft()).isEqualTo(5);
            assertThat(result.getPurchaseRequests().getSubmitted()).isEqualTo(3);
            assertThat(result.getPurchaseRequests().getApproved()).isEqualTo(10);
            assertThat(result.getPurchaseRequests().getTotal()).isEqualTo(21);
            assertThat(result.getPurchaseOrders().getDraft()).isEqualTo(4);
            assertThat(result.getPurchaseOrders().getTotal()).isEqualTo(21);
            assertThat(result.getMonthlySpending()).isEqualByComparingTo(BigDecimal.valueOf(250000));
            assertThat(result.getPendingApprovals()).isEqualTo(5); // 3 submitted + 2 manager_approved
        }

        @Test
        @DisplayName("should handle null monthly spending")
        void getSummary_NullSpending_ReturnsZero() {
            when(purchaseRequestRepository.countByStatus(any(PurchaseRequestStatus.class))).thenReturn(0L);
            when(purchaseRequestRepository.count()).thenReturn(0L);
            when(purchaseOrderRepository.countByStatus(any(PurchaseOrderStatus.class))).thenReturn(0L);
            when(purchaseOrderRepository.count()).thenReturn(0L);
            when(purchaseOrderRepository.sumTotalAmountByDateRange(any(), any())).thenReturn(null);

            DashboardSummaryResponse result = dashboardService.getSummary();

            assertThat(result.getMonthlySpending()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getMonthlySpending()")
    class MonthlySpendingTests {

        @Test
        @DisplayName("should delegate to repository")
        void getMonthlySpending_DelegatesToRepo() {
            List<MonthlySpendingResponse> expected = List.of(
                    new MonthlySpendingResponse(2026, 1, BigDecimal.valueOf(100000), 5L),
                    new MonthlySpendingResponse(2026, 2, BigDecimal.valueOf(200000), 8L)
            );
            when(purchaseOrderRepository.getMonthlySpending(2026)).thenReturn(expected);

            List<MonthlySpendingResponse> result = dashboardService.getMonthlySpending(2026);

            assertThat(result).hasSize(2);
            verify(purchaseOrderRepository).getMonthlySpending(2026);
        }
    }

    @Nested
    @DisplayName("getTopVendors()")
    class TopVendorsTests {

        @Test
        @DisplayName("should delegate to repository with limit")
        void getTopVendors_DelegatesToRepo() {
            List<TopVendorResponse> expected = List.of(
                    new TopVendorResponse(java.util.UUID.randomUUID(), "ACME", 10L, BigDecimal.valueOf(500000))
            );
            when(purchaseOrderRepository.getTopVendors(10)).thenReturn(expected);

            List<TopVendorResponse> result = dashboardService.getTopVendors(10);

            assertThat(result).hasSize(1);
            verify(purchaseOrderRepository).getTopVendors(10);
        }
    }
}
