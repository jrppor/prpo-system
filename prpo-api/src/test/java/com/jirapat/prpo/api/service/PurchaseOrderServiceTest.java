package com.jirapat.prpo.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.api.dto.request.UpdatePurchaseOrderStatusRequest;
import com.jirapat.prpo.api.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.api.entity.NotificationType;
import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestItem;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.entity.Vendor;
import com.jirapat.prpo.api.exception.BadRequestException;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.mapper.PurchaseOrderMapper;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;
import com.jirapat.prpo.api.repository.VendorRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService Unit Tests")
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private SecurityService securityService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private VendorRepository vendorRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private User currentUser;
    private Vendor testVendor;
    private PurchaseOrder testPo;
    private PurchaseOrderResponse testPoResponse;
    private UUID poId;

    @BeforeEach
    void setUp() {
        poId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).name("PROCUREMENT").build();
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("procurement@example.com")
                .firstName("Proc")
                .lastName("User")
                .role(role)
                .isActive(true)
                .build();

        testVendor = Vendor.builder()
                .id(UUID.randomUUID())
                .code("VEN-001")
                .name("ACME CORP")
                .build();

        testPo = PurchaseOrder.builder()
                .id(poId)
                .poNumber("PO-2569-0001")
                .vendor(testVendor)
                .createdBy(currentUser)
                .status(PurchaseOrderStatus.DRAFT)
                .totalAmount(BigDecimal.valueOf(50000))
                .items(new ArrayList<>())
                .build();

        testPoResponse = PurchaseOrderResponse.builder()
                .id(poId)
                .poNumber("PO-2569-0001")
                .status(PurchaseOrderStatus.DRAFT)
                .totalAmount(BigDecimal.valueOf(50000))
                .build();
    }

    @Nested
    @DisplayName("getPurchaseOrderById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return PO when found")
        void getById_Found_Returns() {
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));
            when(purchaseOrderMapper.toResponse(testPo)).thenReturn(testPoResponse);

            PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrderById(poId);

            assertThat(result.getPoNumber()).isEqualTo("PO-2569-0001");
        }

        @Test
        @DisplayName("should throw when PO not found")
        void getById_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(purchaseOrderRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.getPurchaseOrderById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletePurchaseOrder()")
    class DeleteTests {

        @Test
        @DisplayName("should soft-delete DRAFT PO")
        void delete_Draft_Succeeds() {
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));

            purchaseOrderService.deletePurchaseOrder(poId);

            assertThat(testPo.getDeletedAt()).isNotNull();
            verify(purchaseOrderRepository).save(testPo);
            verify(auditLogService).logDelete(eq("PurchaseOrder"), eq(poId), anyString());
        }

        @Test
        @DisplayName("should throw when deleting non-DRAFT PO")
        void delete_NonDraft_Throws() {
            testPo.setStatus(PurchaseOrderStatus.SENT);
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));

            assertThatThrownBy(() -> purchaseOrderService.deletePurchaseOrder(poId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only DRAFT purchase order can be deleted");
        }
    }

    @Nested
    @DisplayName("updatePurchaseOrderStatus() — valid transitions")
    class ValidStatusTransitionTests {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "DRAFT, SENT",
                "DRAFT, CANCELLED",
                "SENT, RECEIVED",
                "SENT, CANCELLED",
                "RECEIVED, COMPLETED"
        })
        @DisplayName("should allow valid status transitions")
        void validTransition_Succeeds(PurchaseOrderStatus from, PurchaseOrderStatus to) {
            testPo.setStatus(from);
            UpdatePurchaseOrderStatusRequest request = UpdatePurchaseOrderStatusRequest.builder()
                    .status(to)
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));
            when(purchaseOrderRepository.save(testPo)).thenReturn(testPo);
            when(purchaseOrderMapper.toResponse(testPo)).thenReturn(testPoResponse);

            purchaseOrderService.updatePurchaseOrderStatus(poId, request);

            assertThat(testPo.getStatus()).isEqualTo(to);
            verify(auditLogService).logStatusChange("PurchaseOrder", poId, from.name(), to.name());
        }
    }

    @Nested
    @DisplayName("updatePurchaseOrderStatus() — invalid transitions")
    class InvalidStatusTransitionTests {

        @ParameterizedTest(name = "{0} → {1} should fail")
        @CsvSource({
                "COMPLETED, SENT",
                "COMPLETED, CANCELLED",
                "CANCELLED, SENT",
                "CANCELLED, DRAFT",
                "RECEIVED, SENT",
                "RECEIVED, CANCELLED",
                "DRAFT, RECEIVED",
                "DRAFT, COMPLETED"
        })
        @DisplayName("should reject invalid status transitions")
        void invalidTransition_Throws(PurchaseOrderStatus from, PurchaseOrderStatus to) {
            testPo.setStatus(from);
            UpdatePurchaseOrderStatusRequest request = UpdatePurchaseOrderStatusRequest.builder()
                    .status(to)
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));

            assertThatThrownBy(() -> purchaseOrderService.updatePurchaseOrderStatus(poId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot change status from");
        }
    }

    @Nested
    @DisplayName("updatePurchaseOrderStatus() — notifications")
    class StatusNotificationTests {

        @Test
        @DisplayName("should notify PR owner when PO status changes to SENT")
        void statusToSent_NotifiesPrOwner() {
            User prOwner = User.builder().id(UUID.randomUUID()).email("owner@example.com")
                    .role(Role.builder().name("USER").build()).build();
            PurchaseRequest pr = PurchaseRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(prOwner)
                    .build();
            testPo.setPurchaseRequest(pr);
            testPo.setStatus(PurchaseOrderStatus.DRAFT);

            UpdatePurchaseOrderStatusRequest request = UpdatePurchaseOrderStatusRequest.builder()
                    .status(PurchaseOrderStatus.SENT)
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(testPo));
            when(purchaseOrderRepository.save(testPo)).thenReturn(testPo);
            when(purchaseOrderMapper.toResponse(testPo)).thenReturn(testPoResponse);

            purchaseOrderService.updatePurchaseOrderStatus(poId, request);

            verify(notificationService).send(
                    eq(prOwner), eq(NotificationType.PO_SENT),
                    anyString(), anyString(), eq("PurchaseOrder"), eq(poId));
        }
    }

    @Nested
    @DisplayName("createPurchaseOrderFromPr()")
    class CreateFromPrTests {

        @Test
        @DisplayName("should create PO from approved PR")
        void createFromPr_ApprovedPr_Succeeds() {
            UUID prId = UUID.randomUUID();
            PurchaseRequest pr = PurchaseRequest.builder()
                    .id(prId)
                    .prNumber("PR-2569-0001")
                    .status(PurchaseRequestStatus.APPROVED)
                    .requester(currentUser)
                    .items(List.of(
                            PurchaseRequestItem.builder()
                                    .description("Item 1")
                                    .quantity(BigDecimal.TEN)
                                    .unit("pcs")
                                    .estimatedPrice(BigDecimal.valueOf(100))
                                    .totalPrice(BigDecimal.valueOf(1000))
                                    .build()
                    ))
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));
            when(vendorRepository.findById(testVendor.getId())).thenReturn(Optional.of(testVendor));
            when(purchaseOrderRepository.getNextPoNumberSequence()).thenReturn(1L);
            when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(testPo);
            when(purchaseOrderMapper.toResponse(testPo)).thenReturn(testPoResponse);

            PurchaseOrderResponse result = purchaseOrderService.createPurchaseOrderFromPr(prId, testVendor.getId());

            assertThat(result).isNotNull();
            verify(auditLogService).logCreate(eq("PurchaseOrder"), any(), anyString());
            verify(notificationService).send(eq(currentUser), eq(NotificationType.PO_CREATED),
                    anyString(), anyString(), eq("PurchaseOrder"), any());
        }

        @Test
        @DisplayName("should throw when PR is not APPROVED")
        void createFromPr_NotApproved_Throws() {
            UUID prId = UUID.randomUUID();
            PurchaseRequest pr = PurchaseRequest.builder()
                    .id(prId)
                    .status(PurchaseRequestStatus.SUBMITTED)
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));

            assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrderFromPr(prId, testVendor.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only APPROVED purchase requests can be converted to PO");
        }

        @Test
        @DisplayName("should throw when vendor not found")
        void createFromPr_VendorNotFound_Throws() {
            UUID prId = UUID.randomUUID();
            UUID missingVendorId = UUID.randomUUID();
            PurchaseRequest pr = PurchaseRequest.builder()
                    .id(prId)
                    .status(PurchaseRequestStatus.APPROVED)
                    .items(List.of())
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));
            when(vendorRepository.findById(missingVendorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrderFromPr(prId, missingVendorId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
