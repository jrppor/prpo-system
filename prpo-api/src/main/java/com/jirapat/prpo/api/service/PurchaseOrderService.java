package com.jirapat.prpo.api.service;

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

import com.jirapat.prpo.api.dto.request.CreatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.request.PurchaseOrderItemRequest;
import com.jirapat.prpo.api.dto.request.UpdatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.request.UpdatePurchaseOrderStatusRequest;
import com.jirapat.prpo.api.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.api.entity.NotificationType;
import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderItem;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.ReferenceType;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.entity.Vendor;
import com.jirapat.prpo.api.exception.BadRequestException;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.mapper.PurchaseOrderMapper;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;
import com.jirapat.prpo.api.repository.VendorRepository;
import com.jirapat.prpo.api.repository.specification.PurchaseOrderSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final AttachmentService attachmentService;
    private final VendorRepository vendorRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(
        PurchaseOrderStatus status,
        LocalDate dateFrom,
        LocalDate dateTo,
        String search,
        Pageable pageable
    ) {

        Specification<PurchaseOrder> spec = Specification
        .where(PurchaseOrderSpecification.hasStatus(status))
        .and(PurchaseOrderSpecification.createdAfter(dateFrom))
        .and(PurchaseOrderSpecification.createdBefore(dateTo))
        .and(PurchaseOrderSpecification.searchByKeyword(search));

        return purchaseOrderRepository.findAll(spec, pageable)
                .map(purchaseOrderMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public  PurchaseOrderResponse getPurchaseOrderById(UUID id) {
        log.info("Fetching Purchase order by id: {}", id);
        PurchaseOrder purchaseOrder = findPurchaseOrderById(id);
        PurchaseOrderResponse response = purchaseOrderMapper.toResponse(purchaseOrder);
        response.setAttachments(attachmentService.getAttachmentsByReference(
                ReferenceType.PURCHASE_ORDER, id));
        return response;
    }

     public  PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Creating purchase order by user: {}", currentUser.getEmail());

        PurchaseOrder purchaseOrder = purchaseOrderMapper.toEntity(request);

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", request.getVendorId().toString()));
        purchaseOrder.setVendor(vendor);

        if (request.getPurchaseRequestId() != null) {
            PurchaseRequest pr = purchaseRequestRepository.findById(request.getPurchaseRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", request.getPurchaseRequestId().toString()));
            purchaseOrder.setPurchaseRequest(pr);
        }

        purchaseOrder.setCreatedBy(currentUser);
        purchaseOrder.setPoNumber(generatePoNumber());
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);

         mapItems(request.getItems(), purchaseOrder);
         recalculateTotalAmount(purchaseOrder);

         PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
         auditLogService.logCreate("PurchaseOrder", saved.getId(), saved.getPoNumber());
         return purchaseOrderMapper.toResponse(saved);
     }

     public PurchaseOrderResponse updatePurchaseOrder(UUID id, UpdatePurchaseOrderRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Updating purchase order by user: {}", currentUser.getEmail());

        PurchaseOrder purchaseOrder = findPurchaseOrderById(id);

        if(purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase order can be updated");
        }

        purchaseOrderMapper.updateEntity(request, purchaseOrder);

        // Update vendor if changed
        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", request.getVendorId().toString()));
            purchaseOrder.setVendor(vendor);
        }

        // Update purchase request if changed
        if (request.getPurchaseRequestId() != null) {
            PurchaseRequest pr = purchaseRequestRepository.findById(request.getPurchaseRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", request.getPurchaseRequestId().toString()));
            purchaseOrder.setPurchaseRequest(pr);
        } else {
            purchaseOrder.setPurchaseRequest(null);
        }

        // Replace items
        purchaseOrder.getItems().clear();
        entityManager.flush();

        mapItems(request.getItems(), purchaseOrder);
        recalculateTotalAmount(purchaseOrder);

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.logUpdate("PurchaseOrder", saved.getId(), null, saved.getPoNumber());
        return purchaseOrderMapper.toResponse(saved);
     }

     public PurchaseOrderResponse updatePurchaseOrderStatus(UUID id, UpdatePurchaseOrderStatusRequest request) {
        User currentUser = securityService.getCurrentUser();
        log.info("Updating purchase order status: {} to {} by user: {}", id, request.getStatus(), currentUser.getEmail());

        PurchaseOrder purchaseOrder = findPurchaseOrderById(id);
        PurchaseOrderStatus currentStatus = purchaseOrder.getStatus();
        PurchaseOrderStatus newStatus = request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        purchaseOrder.setStatus(newStatus);

        if (request.getRemark() != null) {
            purchaseOrder.setRemark(request.getRemark());
        }

        if (request.getActualDeliveryDate() != null) {
            purchaseOrder.setActualDeliveryDate(request.getActualDeliveryDate());
        }

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.logStatusChange("PurchaseOrder", id, currentStatus.name(), newStatus.name());

        // Notify PR owner when PO status changes
        if (saved.getPurchaseRequest() != null) {
            NotificationType notifType = switch (newStatus) {
                case SENT -> NotificationType.PO_SENT;
                case RECEIVED -> NotificationType.PO_RECEIVED;
                case COMPLETED -> NotificationType.PO_COMPLETED;
                case CANCELLED -> NotificationType.PO_CANCELLED;
                default -> null;
            };
            if (notifType != null) {
                notificationService.send(
                        saved.getPurchaseRequest().getRequester(),
                        notifType,
                        "PO สถานะเปลี่ยน: " + saved.getPoNumber(),
                        "PO " + saved.getPoNumber() + " เปลี่ยนสถานะเป็น " + newStatus.name(),
                        "PurchaseOrder",
                        saved.getId()
                );
            }
        }

        log.info("Purchase order status updated: {} -> {}", currentStatus, newStatus);
        return purchaseOrderMapper.toResponse(saved);
     }

     public PurchaseOrderResponse createPurchaseOrderFromPr(UUID prId, UUID vendorId) {
        User currentUser = securityService.getCurrentUser();
        log.info("Creating purchase order from PR: {} by user: {}", prId, currentUser.getEmail());

        PurchaseRequest pr = purchaseRequestRepository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", prId.toString()));

        if (pr.getStatus() != PurchaseRequestStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED purchase requests can be converted to PO");
        }

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId.toString()));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .purchaseRequest(pr)
                .vendor(vendor)
                .createdBy(currentUser)
                .status(PurchaseOrderStatus.DRAFT)
                .orderDate(LocalDate.now())
                .build();

        // Map PR items to PO items
        AtomicInteger itemNumber = new AtomicInteger(1);
        List<PurchaseOrderItem> poItems = pr.getItems().stream()
                .<PurchaseOrderItem>map(prItem -> PurchaseOrderItem.builder()
                        .purchaseOrder(purchaseOrder)
                        .itemNumber(itemNumber.getAndIncrement())
                        .description(prItem.getDescription())
                        .quantity(prItem.getQuantity())
                        .unit(prItem.getUnit())
                        .unitPrice(prItem.getEstimatedPrice())
                        .totalPrice(prItem.getTotalPrice())
                        .remark(prItem.getRemark())
                        .build())
                .toList();
        purchaseOrder.setItems(new java.util.ArrayList<>(poItems));
        recalculateTotalAmount(purchaseOrder);

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.logCreate("PurchaseOrder", saved.getId(), saved.getPoNumber());

        if (pr.getRequester() != null) {
            notificationService.send(
                    pr.getRequester(),
                    NotificationType.PO_CREATED,
                    "PO สร้างจาก PR: " + pr.getPrNumber(),
                    "PO " + saved.getPoNumber() + " ถูกสร้างจาก PR " + pr.getPrNumber(),
                    "PurchaseOrder",
                    saved.getId()
            );
        }

        log.info("Purchase order created from PR: {} -> {}", pr.getPrNumber(), saved.getPoNumber());
        return purchaseOrderMapper.toResponse(saved);
     }

     public void deletePurchaseOrder(UUID id) {
        log.info("Deleting purchase order: {}", id);

        PurchaseOrder purchaseOrder = findPurchaseOrderById(id);

        if(purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase order can be deleted");
        }

        purchaseOrder.softDelete();
        purchaseOrder.getItems().forEach(PurchaseOrderItem::softDelete);
        purchaseOrderRepository.save(purchaseOrder);
        auditLogService.logDelete("PurchaseOrder", id, purchaseOrder.getPoNumber());
         log.info("Purchase order soft-deleted successfully: {}", id);
     }

     private PurchaseOrder findPurchaseOrderById(UUID id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id.toString()));
     }

    private void validateStatusTransition(PurchaseOrderStatus current, PurchaseOrderStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == PurchaseOrderStatus.SENT || target == PurchaseOrderStatus.CANCELLED;
            case SENT -> target == PurchaseOrderStatus.RECEIVED || target == PurchaseOrderStatus.CANCELLED;
            case RECEIVED -> target == PurchaseOrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    String.format("Cannot change status from %s to %s", current, target));
        }
    }

    private String generatePoNumber() {
        int year = LocalDate.now().getYear() + 543; // พ.ศ.
        Long seq = purchaseOrderRepository.getNextPoNumberSequence();
        return String.format("PO-%d-%04d", year, seq);
    }

    private void mapItems(List<PurchaseOrderItemRequest> itemRequests, PurchaseOrder purchaseOrder) {
        AtomicInteger itemNumber = new AtomicInteger(1);
        List<PurchaseOrderItem> items = itemRequests.stream()
                .map(itemReq -> {
                    PurchaseOrderItem item = purchaseOrderMapper.toItemEntity(itemReq);
                    item.setPurchaseOrder(purchaseOrder);
                    item.setItemNumber(itemNumber.getAndIncrement());
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        item.setTotalPrice(item.getUnitPrice().multiply(item.getQuantity()));
                    }
                    return item;
                })
                .toList();
        purchaseOrder.getItems().addAll(items);
    }

    private void recalculateTotalAmount(PurchaseOrder purchaseOrder) {
        BigDecimal totalAmount = purchaseOrder.getItems().stream()
                .map(PurchaseOrderItem::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchaseOrder.setTotalAmount(totalAmount);
    }
}
