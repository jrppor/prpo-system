package com.jirapat.prpo.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.entity.PurchaseOrder;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    @EntityGraph(attributePaths = {"purchaseRequest", "vendor"})
    Page<PurchaseOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"purchaseRequest", "vendor"})
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    @Query(value = "SELECT nextval('po_number_seq')", nativeQuery = true)
    Long getNextPoNumberSequence();
}
