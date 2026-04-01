package com.jirapat.prpo.repository;

import com.jirapat.prpo.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @EntityGraph(attributePaths = {"purchaseRequest", "vendor"})
    Page<PurchaseOrder> findAll(Pageable pageable);

    @Query(value = "SELECT nextval('po_number_seq')", nativeQuery = true)
    Long getNextPoNumberSequence();
}
