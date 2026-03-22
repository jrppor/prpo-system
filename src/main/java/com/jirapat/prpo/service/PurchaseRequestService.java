package com.jirapat.prpo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseRequestRequest;
import com.jirapat.prpo.dto.response.ApprovalHistoryResponse;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.entity.ApprovalHistory;
import com.jirapat.prpo.entity.PurchaseRequest;
import com.jirapat.prpo.entity.PurchaseRequestItem;
import com.jirapat.prpo.entity.PurchaseRequestStatus;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.ApprovalHistoryMapper;
import com.jirapat.prpo.mapper.PurchaseRequestMapper;
import com.jirapat.prpo.repository.ApprovalHistoryRepository;
import com.jirapat.prpo.repository.PurchaseRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final ApprovalHistoryMapper approvalHistoryMapper;
    private final SecurityService securityService;
    private final jakarta.persistence.EntityManager entityManager;

    public Page<PurchaseRequestResponse> getAllPurchaseRequests(Pageable pageable) {
        return purchaseRequestRepository.findAll(pageable)
                .map(purchaseRequestMapper::toListResponse);
    }

    public PurchaseRequestResponse getPurchaseRequestById(UUID id) {
        log.info("Fetching Purchase request by id: {}", id);
        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);
        return purchaseRequestMapper.toPurchaseRequestResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Creating purchase request by user: {}", currentUser.getEmail());

        PurchaseRequest purchaseRequest = purchaseRequestMapper.toEntity(request);
        purchaseRequest.setPrNumber(generatePrNumber());
        purchaseRequest.setStatus(PurchaseRequestStatus.DRAFT);
        purchaseRequest.setRequester(currentUser);

        mapItems(request.getItems(), purchaseRequest);
        recalculateTotalAmount(purchaseRequest);

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

    @Transactional
    public PurchaseRequestResponse updatePurchaseRequest(UUID id, UpdatePurchaseRequestRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Updating purchase request {} by user: {}", id, currentUser.getEmail());

        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase requests can be updated");
        }

        // Update basic fields
        purchaseRequestMapper.updateEntity(request, purchaseRequest);

        // Replace items — flush after clear to execute DELETEs before INSERTs
        purchaseRequest.getItems().clear();
        entityManager.flush();

        mapItems(request.getItems(), purchaseRequest);
        recalculateTotalAmount(purchaseRequest);

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

    @Transactional
    public void deletePurchaseRequest(UUID id) {
        log.info("Deleting purchase request: {}", id);
        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase requests can be deleted");
        }

        purchaseRequestRepository.delete(purchaseRequest);
        log.info("Purchase request deleted successfully: {}", id);
    }


    // ============ Helper Methods ============
    public PurchaseRequest findPurchaseRequestById(UUID id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id.toString()));
    }

    private String generatePrNumber() {
        int year = LocalDate.now().getYear() + 543; // พ.ศ.
        Long seq = purchaseRequestRepository.getNextPrNumberSequence();
        return String.format("PR-%d-%04d", year, seq);
    }

    private void mapItems(List<com.jirapat.prpo.dto.request.PurchaseRequestItemRequest> itemRequests, PurchaseRequest purchaseRequest) {
        AtomicInteger itemNumber = new AtomicInteger(1);
        List<PurchaseRequestItem> items = itemRequests.stream()
                .map(itemReq -> {
                    PurchaseRequestItem item = purchaseRequestMapper.toItemEntity(itemReq);
                    item.setPurchaseRequest(purchaseRequest);
                    item.setItemNumber(itemNumber.getAndIncrement());
                    if (item.getEstimatedPrice() != null && item.getQuantity() != null) {
                        item.setTotalPrice(item.getEstimatedPrice().multiply(item.getQuantity()));
                    }
                    return item;
                })
                .toList();
        purchaseRequest.getItems().addAll(items);
    }

    private void recalculateTotalAmount(PurchaseRequest purchaseRequest) {
        BigDecimal totalAmount = purchaseRequest.getItems().stream()
                .map(PurchaseRequestItem::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchaseRequest.setTotalAmount(totalAmount);
    }

    public List<ApprovalHistoryResponse> getApprovalHistories(UUID purchaseRequestId) {
        findPurchaseRequestById(purchaseRequestId);
        List<ApprovalHistory> histories = approvalHistoryRepository
                .findByPrId(purchaseRequestId);
        return histories.stream()
                .map(approvalHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public PurchaseRequestResponse submitPurchaseRequest(UUID id) {
        User currentUser = securityService.getCurrentUser();
        log.info("Submitting purchase request {} by user: {}", id, currentUser.getEmail());

        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase requests can be submitted");
        }

        purchaseRequest.setStatus(PurchaseRequestStatus.SUBMITTED);
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }
}
