package com.jirapat.prpo.api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.api.dto.response.MonthlySpendingResponse;
import com.jirapat.prpo.api.dto.response.TopVendorResponse;
import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    @EntityGraph(attributePaths = {"purchaseRequest", "vendor"})
    Page<PurchaseOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"purchaseRequest", "vendor"})
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    @Query(value = "SELECT nextval('po_number_seq')", nativeQuery = true)
    Long getNextPoNumberSequence();

    long countByStatus(PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE po.createdAt BETWEEN :from AND :to")
    BigDecimal sumTotalAmountByDateRange(LocalDateTime from, LocalDateTime to);

    @Query("SELECT new com.jirapat.prpo.api.dto.response.MonthlySpendingResponse(" +
            "YEAR(po.createdAt), MONTH(po.createdAt), COALESCE(SUM(po.totalAmount), 0), COUNT(po)) " +
            "FROM PurchaseOrder po WHERE YEAR(po.createdAt) = :year " +
            "GROUP BY YEAR(po.createdAt), MONTH(po.createdAt) " +
            "ORDER BY MONTH(po.createdAt)")
    List<MonthlySpendingResponse> getMonthlySpending(int year);

    @Query("SELECT new com.jirapat.prpo.api.dto.response.TopVendorResponse(" +
            "po.vendor.id, po.vendor.name, COUNT(po), COALESCE(SUM(po.totalAmount), 0)) " +
            "FROM PurchaseOrder po " +
            "GROUP BY po.vendor.id, po.vendor.name " +
            "ORDER BY SUM(po.totalAmount) DESC " +
            "LIMIT :limit")
    List<TopVendorResponse> getTopVendors(int limit);
}
