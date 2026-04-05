package com.jirapat.prpo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.entity.PurchaseRequest;
import com.jirapat.prpo.entity.PurchaseRequestStatus;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    @EntityGraph(attributePaths = "requester")
    Page<PurchaseRequest> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "requester")
    Page<PurchaseRequest> findAll(Specification<PurchaseRequest> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "items"})
    Optional<PurchaseRequest> findById(UUID id);

    // Load requester eagerly for list responses to avoid LazyInitializationException during mapping.
    // If you prefer an explicit query instead, use:
    // @EntityGraph(attributePaths = "requester")
    // @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = :status")
    @EntityGraph(attributePaths = "requester")
    Page<PurchaseRequest> findByStatus(PurchaseRequestStatus status, Pageable pageable);

    // @Query("SELECT pr FROM PurchaseRequest pr JOIN FETCH pr.requester")
    // Page<PurchaseRequest> findAllWithRequester(Pageable pageable);

    @Query(value = "SELECT nextval('pr_number_seq')", nativeQuery = true)
    Long getNextPrNumberSequence();
}
