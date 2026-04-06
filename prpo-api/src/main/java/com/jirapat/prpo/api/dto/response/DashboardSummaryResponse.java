package com.jirapat.prpo.api.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {

    private StatusCount purchaseRequests;
    private StatusCount purchaseOrders;
    private BigDecimal monthlySpending;
    private long pendingApprovals;

    @Data
    @Builder
    public static class StatusCount {
        private long draft;
        private long submitted;
        private long approved;
        private long rejected;
        private long managerApproved;
        private long sent;
        private long received;
        private long completed;
        private long cancelled;
        private long total;
    }
}
