package com.jirapat.prpo.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.api.dto.request.CreatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.request.UpdatePurchaseOrderRequest;
import com.jirapat.prpo.api.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.entity.Vendor;
import com.jirapat.prpo.api.exception.BadRequestException;
import com.jirapat.prpo.api.mapper.PurchaseOrderMapper;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;
import com.jirapat.prpo.api.repository.VendorRepository;

import jakarta.persistence.EntityManager;


@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService — กันแปลง PR->PO ซ้ำ")
class PurchaseOrderFromPrDuplicateGuardTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private SecurityService securityService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private AttachmentService attachmentService;
    @Mock private VendorRepository vendorRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private User currentUser;
    private Vendor testVendor;
    private PurchaseOrder testPo;
    private PurchaseRequest approvedPr;
    private UUID prId;

    @BeforeEach
    void setUp() {
        prId = UUID.randomUUID();
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("procurement@example.com")
                .firstName("Proc")
                .lastName("User")
                .role(Role.builder().id(UUID.randomUUID()).name("PROCUREMENT").build())
                .isActive(true)
                .build();

        testVendor = Vendor.builder()
                .id(UUID.randomUUID())
                .code("VEN-001")
                .name("ACME CORP")
                .build();

        testPo = PurchaseOrder.builder()
                .id(UUID.randomUUID())
                .poNumber("PO-2569-0001")
                .vendor(testVendor)
                .createdBy(currentUser)
                .status(PurchaseOrderStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        approvedPr = PurchaseRequest.builder()
                .id(prId)
                .prNumber("PR-2569-0001")
                .status(PurchaseRequestStatus.APPROVED)
                .requester(currentUser)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("PR ที่มี PO ผูกอยู่แล้ว — แปลงซ้ำต้องโยน BadRequestException")
    void prAlreadyConverted_throwsBadRequest() {
        when(securityService.getCurrentUser()).thenReturn(currentUser);
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(approvedPr));
        when(purchaseOrderRepository.existsByPurchaseRequestId(prId)).thenReturn(true);

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrderFromPr(prId, testVendor.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been converted");

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("PR ที่ยังไม่เคยแปลง — แปลงเป็น PO ได้สำเร็จ (เช็ค guard ก่อน)")
    void prNotYetConverted_succeeds() {
        when(securityService.getCurrentUser()).thenReturn(currentUser);
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(approvedPr));
        when(purchaseOrderRepository.existsByPurchaseRequestId(prId)).thenReturn(false);
        when(vendorRepository.findById(testVendor.getId())).thenReturn(Optional.of(testVendor));
        when(purchaseOrderRepository.getNextPoNumberSequence()).thenReturn(1L);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(testPo);
        when(purchaseOrderMapper.toResponse(testPo)).thenReturn(
                PurchaseOrderResponse.builder().id(testPo.getId()).poNumber("PO-2569-0001").build());

        purchaseOrderService.createPurchaseOrderFromPr(prId, testVendor.getId());

        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
        verify(purchaseOrderRepository).existsByPurchaseRequestId(prId);
    }

    @Test
    @DisplayName("createPurchaseOrder (manual) ผูก PR ที่แปลงไปแล้ว ต้องโยน BadRequestException")
    void manualCreate_withConvertedPr_throwsBadRequest() {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(testVendor.getId())
                .purchaseRequestId(prId)
                .build();

        when(securityService.getCurrentUser()).thenReturn(currentUser);
        when(purchaseOrderMapper.toEntity(request)).thenReturn(
                PurchaseOrder.builder().items(new ArrayList<>()).build());
        when(vendorRepository.findById(testVendor.getId())).thenReturn(Optional.of(testVendor));
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(approvedPr));
        when(purchaseOrderRepository.existsByPurchaseRequestId(prId)).thenReturn(true);

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been converted");

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("updatePurchaseOrder ผูก PR ที่ PO อื่นถือไว้แล้ว ต้องโยน BadRequestException")
    void update_linkingConvertedPr_throwsBadRequest() {
        UUID poId = testPo.getId();
        UpdatePurchaseOrderRequest request = UpdatePurchaseOrderRequest.builder()
                .purchaseRequestId(prId)
                .build();

        when(securityService.getCurrentUser()).thenReturn(currentUser);
        when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(approvedPr));
        when(purchaseOrderRepository.existsByPurchaseRequestIdAndIdNot(prId, poId)).thenReturn(true);

        assertThatThrownBy(() -> purchaseOrderService.updatePurchaseOrder(poId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been converted");

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }
}
