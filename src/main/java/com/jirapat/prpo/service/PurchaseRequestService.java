package com.jirapat.prpo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseRequestRequest;
import com.jirapat.prpo.dto.response.ApprovalHistoryResponse;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.entity.ApprovalHistory;
import com.jirapat.prpo.entity.NotificationType;
import com.jirapat.prpo.entity.PurchaseRequest;
import com.jirapat.prpo.entity.PurchaseRequestItem;
import com.jirapat.prpo.entity.PurchaseRequestStatus;
import com.jirapat.prpo.entity.Role;
import com.jirapat.prpo.entity.User;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.ApprovalHistoryMapper;
import com.jirapat.prpo.mapper.PurchaseRequestMapper;
import com.jirapat.prpo.repository.ApprovalHistoryRepository;
import com.jirapat.prpo.repository.PurchaseRequestRepository;
import com.jirapat.prpo.repository.specification.PurchaseRequestSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final ApprovalHistoryMapper approvalHistoryMapper;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getAllPurchaseRequests(
            PurchaseRequestStatus status,
            String department,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            Pageable pageable) {

        Specification<PurchaseRequest> spec = Specification
                .where(PurchaseRequestSpecification.hasStatus(status))
                .and(PurchaseRequestSpecification.hasDepartment(department))
                .and(PurchaseRequestSpecification.createdAfter(dateFrom))
                .and(PurchaseRequestSpecification.createdBefore(dateTo))
                .and(PurchaseRequestSpecification.searchByKeyword(search));

        return purchaseRequestRepository.findAll(spec, pageable)
                .map(purchaseRequestMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse getPurchaseRequestById(UUID id) {
        log.info("Fetching Purchase request by id: {}", id);
        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);
        return purchaseRequestMapper.toPurchaseRequestResponse(purchaseRequest);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getPendingApproval(Pageable pageable) {
        User currentUser = securityService.getCurrentUser();
        PurchaseRequestStatus pendingStatus = getPendingApprovalStatus(currentUser.getRole());

        return purchaseRequestRepository.findByStatus(pendingStatus, pageable)
                .map(purchaseRequestMapper::toListResponse);
    }

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
        auditLogService.logCreate("PurchaseRequest", saved.getId(), saved.getPrNumber());
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

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
        auditLogService.logUpdate("PurchaseRequest", saved.getId(), null, saved.getPrNumber());
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

    public void deletePurchaseRequest(UUID id) {
        log.info("Deleting purchase request: {}", id);
        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase requests can be deleted");
        }

        purchaseRequest.softDelete();
        purchaseRequest.getItems().forEach(PurchaseRequestItem::softDelete);
        purchaseRequestRepository.save(purchaseRequest);
        auditLogService.logDelete("PurchaseRequest", id, purchaseRequest.getPrNumber());
        log.info("Purchase request soft-deleted successfully: {}", id);
    }

    public PurchaseRequestResponse submitPurchaseRequest(UUID id) {
        User currentUser = securityService.getCurrentUser();
        log.info("Submitting purchase request {} by user: {}", id, currentUser.getEmail());

        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase requests can be submitted");
        }

        purchaseRequest.setStatus(PurchaseRequestStatus.SUBMITTED);
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        auditLogService.logStatusChange("PurchaseRequest", id, "DRAFT", "SUBMITTED");
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistoryResponse> getApprovalHistories(UUID purchaseRequestId) {
        findPurchaseRequestById(purchaseRequestId);
        List<ApprovalHistory> histories = approvalHistoryRepository
                .findByPrId(purchaseRequestId);
        return histories.stream()
                .map(approvalHistoryMapper::toResponse)
                .toList();
    }

    public PurchaseRequestResponse approvePurchaseRequest(UUID id) {
        User currentUser = securityService.getCurrentUser();
        log.info("Approve purchase request {} by user: {}", id, currentUser.getEmail());

        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        String oldStatus = purchaseRequest.getStatus().name();
        purchaseRequest.setStatus(PurchaseRequestStatus.APPROVED);
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        auditLogService.logStatusChange("PurchaseRequest", id, oldStatus, "APPROVED");
        notificationService.send(
                purchaseRequest.getRequester(),
                NotificationType.PR_APPROVED,
                "PR อนุมัติแล้ว: " + purchaseRequest.getPrNumber(),
                "PR " + purchaseRequest.getTitle() + " ได้รับการอนุมัติ",
                "PurchaseRequest",
                purchaseRequest.getId()
        );
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
    }

    public PurchaseRequestResponse rejectPurchaseRequest(UUID id) {
        User currentUser = securityService.getCurrentUser();
        log.info("Rejecting purchase request {} by user: {}", id, currentUser.getEmail());

        PurchaseRequest purchaseRequest = findPurchaseRequestById(id);

        String oldStatus = purchaseRequest.getStatus().name();
        purchaseRequest.setStatus(PurchaseRequestStatus.REJECTED);
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        auditLogService.logStatusChange("PurchaseRequest", id, oldStatus, "REJECTED");
        notificationService.send(
                purchaseRequest.getRequester(),
                NotificationType.PR_REJECTED,
                "PR ถูกปฏิเสธ: " + purchaseRequest.getPrNumber(),
                "PR " + purchaseRequest.getTitle() + " ถูกปฏิเสธ",
                "PurchaseRequest",
                purchaseRequest.getId()
        );
        return purchaseRequestMapper.toPurchaseRequestResponse(saved);
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

    private PurchaseRequestStatus getPendingApprovalStatus(Role role) {
        if (role == null || role.getName() == null) {
            throw new IllegalStateException("Current user role is not configured");
        }

        return switch (role.getName()) {
            case "MANAGER" -> PurchaseRequestStatus.SUBMITTED;
            case "APPROVER" -> PurchaseRequestStatus.MANAGER_APPROVED;
            default -> throw new IllegalStateException("Only MANAGER or APPROVER can view pending approvals");
        };
    }




}
