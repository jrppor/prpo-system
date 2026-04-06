package com.jirapat.prpo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.dto.request.PurchaseRequestItemRequest;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
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

import jakarta.persistence.EntityManager;

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
    @DisplayName("approvePurchaseRequest()")
    class ApproveTests {

        @Test
        @DisplayName("should approve and notify requester")
        void approve_Valid_ApprovesAndNotifies() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            when(purchaseRequestRepository.save(testPr)).thenReturn(testPr);
            when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);

            purchaseRequestService.approvePurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
            verify(auditLogService).logStatusChange("PurchaseRequest", prId, "SUBMITTED", "APPROVED");
            verify(notificationService).send(
                    eq(currentUser), eq(NotificationType.PR_APPROVED),
                    anyString(), anyString(), eq("PurchaseRequest"), eq(prId));
        }
    }

    @Nested
    @DisplayName("rejectPurchaseRequest()")
    class RejectTests {

        @Test
        @DisplayName("should reject and notify requester")
        void reject_Valid_RejectsAndNotifies() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            when(securityService.getCurrentUser()).thenReturn(currentUser);
            when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
            when(purchaseRequestRepository.save(testPr)).thenReturn(testPr);
            when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);

            purchaseRequestService.rejectPurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
            verify(notificationService).send(
                    eq(currentUser), eq(NotificationType.PR_REJECTED),
                    anyString(), anyString(), eq("PurchaseRequest"), eq(prId));
        }
    }

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
