package com.jirapat.prpo.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jirapat.prpo.api.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.api.dto.request.PurchaseRequestItemRequest;
import com.jirapat.prpo.api.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestItem;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.exception.UnauthorizedException;
import com.jirapat.prpo.api.mapper.ApprovalHistoryMapper;
import com.jirapat.prpo.api.mapper.PurchaseRequestMapper;
import com.jirapat.prpo.api.repository.ApprovalHistoryRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestService Unit Tests")
class PurchaseRequestServiceTest {

    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock private PurchaseRequestMapper purchaseRequestMapper;
    @Mock private ApprovalHistoryMapper approvalHistoryMapper;
    @Mock private SecurityService securityService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private AttachmentService attachmentService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private PurchaseRequestService purchaseRequestService;

    private User currentUser;
    private PurchaseRequest testPr;
    private PurchaseRequestResponse testPrResponse;
    private UUID prId;

    @BeforeEach
    void setUp() {
        prId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).name("USER").build();
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .isActive(true)
                .build();

        testPr = PurchaseRequest.builder()
                .id(prId)
                .prNumber("PR-2569-0001")
                .title("Office Supplies")
                .status(PurchaseRequestStatus.DRAFT)
                .totalAmount(BigDecimal.valueOf(10000))
                .department("IT")
                .requester(currentUser)
                .items(new ArrayList<>())
                .build();

        testPrResponse = PurchaseRequestResponse.builder()
                .id(prId)
                .prNumber("PR-2569-0001")
                .title("Office Supplies")
                .status(PurchaseRequestStatus.DRAFT)
                .totalAmount(BigDecimal.valueOf(10000))
                .build();
    }

    @Nested
    @DisplayName("getPurchaseRequestById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return PR when found")
        void getById_Found_Returns() {
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);
            when(attachmentService.getAttachmentsByReference(any(), eq(prId))).thenReturn(List.of());

            PurchaseRequestResponse result = purchaseRequestService.getPurchaseRequestById(prId);

            assertThat(result.getPrNumber()).isEqualTo("PR-2569-0001");
        }

        @Test
        @DisplayName("should throw when PR not found")
        void getById_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(purchaseRequestRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseRequestService.getPurchaseRequestById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createPurchaseRequest()")
    class CreateTests {

        @Test
        @DisplayName("should create PR with items and calculate total")
        void create_Valid_Returns() {
            PurchaseRequestItemRequest itemReq = PurchaseRequestItemRequest.builder()
                    .description("Paper A4")
                    .quantity(BigDecimal.valueOf(10))
                    .unit("Ream")
                    .estimatedPrice(BigDecimal.valueOf(150))
                    .build();

            CreatePurchaseRequestRequest request = CreatePurchaseRequestRequest.builder()
                    .title("Office Supplies")
                    .department("IT")
                    .requiredDate(LocalDate.now().plusDays(7))
                    .items(List.of(itemReq))
                    .build();

            PurchaseRequest newPr = PurchaseRequest.builder()
                    .id(prId)
                    .title("Office Supplies")
                    .items(new ArrayList<>())
                    .build();

            PurchaseRequestItem mappedItem = PurchaseRequestItem.builder()
                    .description("Paper A4")
                    .quantity(BigDecimal.valueOf(10))
                    .estimatedPrice(BigDecimal.valueOf(150))
                    .build();

            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestMapper.toEntity(request)).thenReturn(newPr);
            when(purchaseRequestRepository.getNextPrNumberSequence()).thenReturn(1L);
            when(purchaseRequestMapper.toItemEntity(itemReq)).thenReturn(mappedItem);
            when(purchaseRequestRepository.save(any(PurchaseRequest.class))).thenReturn(testPr);
            when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);

            PurchaseRequestResponse result = purchaseRequestService.createPurchaseRequest(request);

            assertThat(result).isNotNull();
            assertThat(result.getPrNumber()).isEqualTo("PR-2569-0001");
            verify(purchaseRequestRepository).save(any(PurchaseRequest.class));
            verify(auditLogService).logCreate(eq("PurchaseRequest"), eq(prId), anyString());
        }
    }

    @Nested
    @DisplayName("deletePurchaseRequest()")
    class DeleteTests {

        @Test
        @DisplayName("should soft-delete DRAFT PR")
        void delete_Draft_Succeeds() {
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));

            purchaseRequestService.deletePurchaseRequest(prId);

            assertThat(testPr.getDeletedAt()).isNotNull();
            verify(securityService).verifyOwnershipOrAdmin(testPr.getRequester().getId());
            verify(purchaseRequestRepository).save(testPr);
            verify(auditLogService).logDelete(eq("PurchaseRequest"), eq(prId), anyString());
        }

        @Test
        @DisplayName("should throw when deleting non-DRAFT PR")
        void delete_NonDraft_Throws() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));

            assertThatThrownBy(() -> purchaseRequestService.deletePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only DRAFT purchase requests can be deleted");
        }
    }

    @Nested
    @DisplayName("submitPurchaseRequest()")
    class SubmitTests {

        @Test
        @DisplayName("should change status from DRAFT to SUBMITTED")
        void submit_Draft_ChangesToSubmitted() {
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            when(purchaseRequestRepository.save(testPr)).thenReturn(testPr);
            when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);

            purchaseRequestService.submitPurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.SUBMITTED);
            verify(auditLogService).logStatusChange("PurchaseRequest", prId, "DRAFT", "SUBMITTED");
        }

        @Test
        @DisplayName("should throw when submitting non-DRAFT")
        void submit_NonDraft_Throws() {
            testPr.setStatus(PurchaseRequestStatus.APPROVED);
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));

            assertThatThrownBy(() -> purchaseRequestService.submitPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("update/delete — ownership check")
    class OwnershipTests {

        @Test
        @DisplayName("delete โดยผู้ที่ไม่ใช่เจ้าของ/ADMIN ถูกปฏิเสธ และไม่มีการ save")
        void delete_byNonOwner_throws() {
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            doThrow(new UnauthorizedException("no permission"))
                    .when(securityService).verifyOwnershipOrAdmin(testPr.getRequester().getId());

            assertThatThrownBy(() -> purchaseRequestService.deletePurchaseRequest(prId))
                    .isInstanceOf(UnauthorizedException.class);
            verify(purchaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("update โดยผู้ที่ไม่ใช่เจ้าของ/ADMIN ถูกปฏิเสธ และไม่มีการ save")
        void update_byNonOwner_throws() {
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            doThrow(new UnauthorizedException("no permission"))
                    .when(securityService).verifyOwnershipOrAdmin(testPr.getRequester().getId());

            assertThatThrownBy(() -> purchaseRequestService.updatePurchaseRequest(prId, null))
                    .isInstanceOf(UnauthorizedException.class);
            verify(purchaseRequestRepository, never()).save(any());
        }
    }

    // หมายเหตุ: approve/reject ครอบคลุมใน PurchaseRequestApprovalFlowTest (workflow 2 ชั้น)

    @Nested
    @DisplayName("getPendingApproval()")
    class PendingApprovalTests {

        @Test
        @DisplayName("MANAGER should see SUBMITTED PRs")
        void pending_Manager_SeesSubmitted() {
            Role managerRole = Role.builder().id(UUID.randomUUID()).name("MANAGER").build();
            User manager = User.builder().id(UUID.randomUUID()).email("mgr@example.com").role(managerRole).build();
            when(securityService.getCurrentUser()).thenReturn(manager);
            when(purchaseRequestRepository.findByStatus(eq(PurchaseRequestStatus.SUBMITTED), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            purchaseRequestService.getPendingApproval(org.springframework.data.domain.PageRequest.of(0, 10));

            verify(purchaseRequestRepository).findByStatus(eq(PurchaseRequestStatus.SUBMITTED), any());
        }

        @Test
        @DisplayName("APPROVER should see MANAGER_APPROVED PRs")
        void pending_Approver_SeesManagerApproved() {
            Role approverRole = Role.builder().id(UUID.randomUUID()).name("APPROVER").build();
            User approver = User.builder().id(UUID.randomUUID()).email("appr@example.com").role(approverRole).build();
            when(securityService.getCurrentUser()).thenReturn(approver);
            when(purchaseRequestRepository.findByStatus(eq(PurchaseRequestStatus.MANAGER_APPROVED), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            purchaseRequestService.getPendingApproval(org.springframework.data.domain.PageRequest.of(0, 10));

            verify(purchaseRequestRepository).findByStatus(eq(PurchaseRequestStatus.MANAGER_APPROVED), any());
        }

        @Test
        @DisplayName("USER role should throw")
        void pending_User_Throws() {
            when(securityService.getCurrentUser()).thenReturn(currentUser);

            assertThatThrownBy(() -> purchaseRequestService.getPendingApproval(
                    org.springframework.data.domain.PageRequest.of(0, 10)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only MANAGER or APPROVER can view pending approvals");
        }
    }
}
