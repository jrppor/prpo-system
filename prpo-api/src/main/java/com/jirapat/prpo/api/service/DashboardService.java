package com.jirapat.prpo.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.api.dto.response.DashboardSummaryResponse;
import com.jirapat.prpo.api.dto.response.MonthlySpendingResponse;
import com.jirapat.prpo.api.dto.response.TopVendorResponse;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DashboardSummaryResponse getSummary() {
        DashboardSummaryResponse.StatusCount prCounts = DashboardSummaryResponse.StatusCount.builder()
                .draft(countPrByStatus(PurchaseRequestStatus.DRAFT))
                .submitted(countPrByStatus(PurchaseRequestStatus.SUBMITTED))
                .managerApproved(countPrByStatus(PurchaseRequestStatus.MANAGER_APPROVED))
                .approved(countPrByStatus(PurchaseRequestStatus.APPROVED))
                .rejected(countPrByStatus(PurchaseRequestStatus.REJECTED))
                .total(purchaseRequestRepository.count())
                .build();

        DashboardSummaryResponse.StatusCount poCounts = DashboardSummaryResponse.StatusCount.builder()
                .draft(countPoByStatus(PurchaseOrderStatus.DRAFT))
                .sent(countPoByStatus(PurchaseOrderStatus.SENT))
                .received(countPoByStatus(PurchaseOrderStatus.RECEIVED))
                .completed(countPoByStatus(PurchaseOrderStatus.COMPLETED))
                .cancelled(countPoByStatus(PurchaseOrderStatus.CANCELLED))
                .total(purchaseOrderRepository.count())
                .build();

        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.atTime(LocalTime.MAX);
        BigDecimal monthlySpending = purchaseOrderRepository.sumTotalAmountByDateRange(monthStart, monthEnd);

        long pendingApprovals = countPrByStatus(PurchaseRequestStatus.SUBMITTED)
                + countPrByStatus(PurchaseRequestStatus.MANAGER_APPROVED);

        return DashboardSummaryResponse.builder()
                .purchaseRequests(prCounts)
                .purchaseOrders(poCounts)
                .monthlySpending(monthlySpending != null ? monthlySpending : BigDecimal.ZERO)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    public List<MonthlySpendingResponse> getMonthlySpending(int year) {
        return purchaseOrderRepository.getMonthlySpending(year);
    }

    public List<TopVendorResponse> getTopVendors(int limit) {
        return purchaseOrderRepository.getTopVendors(limit);
    }

    private long countPrByStatus(PurchaseRequestStatus status) {
        return purchaseRequestRepository.countByStatus(status);
    }

    private long countPoByStatus(PurchaseOrderStatus status) {
        return purchaseOrderRepository.countByStatus(status);
    }
}
