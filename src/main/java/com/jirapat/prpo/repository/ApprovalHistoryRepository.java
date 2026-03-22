package com.jirapat.prpo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jirapat.prpo.entity.ApprovalHistory;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    @Query("SELECT ah FROM ApprovalHistory ah JOIN FETCH ah.approver WHERE ah.purchaseRequest.id = :prId ORDER BY ah.approvalLevel ASC")
    List<ApprovalHistory> findByPrId(UUID prId);
    //findByPurchaseRequestIdOrderByApprovalLevelAsc
}
