package com.jirapat.prpo.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.api.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.api.entity.ApprovalAction;
import com.jirapat.prpo.api.entity.ApprovalHistory;
import com.jirapat.prpo.api.entity.NotificationType;
import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.Role;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.mapper.ApprovalHistoryMapper;
import com.jirapat.prpo.api.mapper.PurchaseRequestMapper;
import com.jirapat.prpo.api.repository.ApprovalHistoryRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;

import jakarta.persistence.EntityManager;


@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestService — approval flow (2-level)")
class PurchaseRequestApprovalFlowTest {

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

    private User requester;
    private User manager;
    private User approver;
    private PurchaseRequest testPr;
    private PurchaseRequestResponse testPrResponse;
    private UUID prId;

    @BeforeEach
    void setUp() {
        prId = UUID.randomUUID();
        requester = userWithRole("USER", "requester@example.com");
        manager = userWithRole("MANAGER", "manager@example.com");
        approver = userWithRole("APPROVER", "approver@example.com");

        testPr = PurchaseRequest.builder()
                .id(prId)
                .prNumber("PR-2569-0001")
                .title("Office Supplies")
                .status(PurchaseRequestStatus.SUBMITTED)
                .totalAmount(BigDecimal.valueOf(10000))
                .department("IT")
                .requester(requester)
                .items(new ArrayList<>())
                .build();

        testPrResponse = PurchaseRequestResponse.builder()
                .id(prId)
                .prNumber("PR-2569-0001")
                .build();
    }

    private static User userWithRole(String roleName, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName(roleName)
                .role(Role.builder().id(UUID.randomUUID()).name(roleName).build())
                .isActive(true)
                .build();
    }

    /** Stub ครบสำหรับเส้นทาง approve/reject ที่ดำเนินจนถึง save. */
    private void stubFullFlow(User actingUser) {
        when(securityService.getCurrentUser()).thenReturn(actingUser);
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
        when(purchaseRequestRepository.save(testPr)).thenReturn(testPr);
        when(purchaseRequestMapper.toPurchaseRequestResponse(testPr)).thenReturn(testPrResponse);
    }

    /** Stub เฉพาะส่วนที่ใช้ก่อนจะโยน exception (ยังไม่ถึง save). */
    private void stubUntilGuard(User actingUser) {
        when(securityService.getCurrentUser()).thenReturn(actingUser);
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(testPr));
    }

    @Nested
    @DisplayName("approvePurchaseRequest() — workflow 2 ชั้น")
    class TwoLevelApproval {

        @Test
        @DisplayName("MANAGER อนุมัติ SUBMITTED -> MANAGER_APPROVED (ยังไม่แจ้งผู้ขอ)")
        void managerApprovesSubmitted_movesToManagerApproved() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubFullFlow(manager);

            purchaseRequestService.approvePurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.MANAGER_APPROVED);
            verify(auditLogService).logStatusChange(
                    "PurchaseRequest", prId, "SUBMITTED", "MANAGER_APPROVED");
            // ชั้น MANAGER ยังไม่ถือว่าจบ — ห้ามแจ้งผู้ขอว่า "อนุมัติแล้ว"
            verify(notificationService, never()).send(
                    any(), eq(NotificationType.PR_APPROVED),
                    anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("APPROVER อนุมัติ MANAGER_APPROVED -> APPROVED + แจ้งผู้ขอ")
        void approverApprovesManagerApproved_movesToApproved() {
            testPr.setStatus(PurchaseRequestStatus.MANAGER_APPROVED);
            stubFullFlow(approver);

            purchaseRequestService.approvePurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
            verify(auditLogService).logStatusChange(
                    "PurchaseRequest", prId, "MANAGER_APPROVED", "APPROVED");
            verify(notificationService).send(
                    eq(requester), eq(NotificationType.PR_APPROVED),
                    anyString(), anyString(), eq("PurchaseRequest"), eq(prId));
        }

        @Test
        @DisplayName("บันทึก ApprovalHistory: ชั้น MANAGER = level 1, action APPROVED")
        void managerApproval_recordsHistoryAtLevel1() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubFullFlow(manager);

            purchaseRequestService.approvePurchaseRequest(prId);

            ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
            verify(approvalHistoryRepository).save(captor.capture());
            ApprovalHistory history = captor.getValue();
            assertThat(history.getAction()).isEqualTo(ApprovalAction.APPROVED);
            assertThat(history.getApprovalLevel()).isEqualTo(1);
            assertThat(history.getApprover()).isEqualTo(manager);
            assertThat(history.getPurchaseRequest()).isEqualTo(testPr);
            assertThat(history.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("บันทึก ApprovalHistory: ชั้น APPROVER = level 2, action APPROVED")
        void approverApproval_recordsHistoryAtLevel2() {
            testPr.setStatus(PurchaseRequestStatus.MANAGER_APPROVED);
            stubFullFlow(approver);

            purchaseRequestService.approvePurchaseRequest(prId);

            ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
            verify(approvalHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getApprovalLevel()).isEqualTo(2);
            assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.APPROVED);
        }
    }

    @Nested
    @DisplayName("approvePurchaseRequest() — ปฏิเสธ status/role ที่ไม่ถูกต้อง")
    class ApprovalGuards {

        @Test
        @DisplayName("approve PR ที่ยังเป็น DRAFT ต้องโยน IllegalStateException")
        void approveDraft_throws() {
            testPr.setStatus(PurchaseRequestStatus.DRAFT);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
            verify(purchaseRequestRepository, never()).save(any());
            verify(approvalHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("approve PR ที่ REJECTED ไปแล้ว ต้องโยน IllegalStateException")
        void approveRejected_throws() {
            testPr.setStatus(PurchaseRequestStatus.REJECTED);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("approve PR ที่ APPROVED ไปแล้ว (approve ซ้ำ) ต้องโยน IllegalStateException")
        void approveAlreadyApproved_throws() {
            testPr.setStatus(PurchaseRequestStatus.APPROVED);
            stubUntilGuard(approver);

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("APPROVER อนุมัติ SUBMITTED ไม่ได้ (ต้องรอ MANAGER ก่อน)")
        void approverApprovesSubmitted_throws() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubUntilGuard(approver);

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("MANAGER อนุมัติ MANAGER_APPROVED ไม่ได้ (เป็นขั้นของ APPROVER)")
        void managerApprovesManagerApproved_throws() {
            testPr.setStatus(PurchaseRequestStatus.MANAGER_APPROVED);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("MANAGER กด approve ซ้ำติด ๆ — ครั้งที่สองถูกปฏิเสธ (idempotency)")
        void doubleApproveByManager_secondCallThrows() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubFullFlow(manager);

            purchaseRequestService.approvePurchaseRequest(prId); // SUBMITTED -> MANAGER_APPROVED

            assertThatThrownBy(() -> purchaseRequestService.approvePurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
            // save ถูกเรียกครั้งเดียว (จากการอนุมัติที่สำเร็จเท่านั้น)
            verify(purchaseRequestRepository, times(1)).save(testPr);
        }
    }

    @Nested
    @DisplayName("rejectPurchaseRequest()")
    class RejectFlow {

        @Test
        @DisplayName("ปฏิเสธ SUBMITTED -> REJECTED + แจ้งผู้ขอ + บันทึก history level 1")
        void rejectSubmitted_movesToRejected() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubFullFlow(manager);

            purchaseRequestService.rejectPurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
            verify(auditLogService).logStatusChange("PurchaseRequest", prId, "SUBMITTED", "REJECTED");
            verify(notificationService).send(
                    eq(requester), eq(NotificationType.PR_REJECTED),
                    anyString(), anyString(), eq("PurchaseRequest"), eq(prId));

            ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
            verify(approvalHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.REJECTED);
            assertThat(captor.getValue().getApprovalLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("ปฏิเสธ MANAGER_APPROVED -> REJECTED + บันทึก history level 2")
        void rejectManagerApproved_movesToRejected() {
            testPr.setStatus(PurchaseRequestStatus.MANAGER_APPROVED);
            stubFullFlow(approver);

            purchaseRequestService.rejectPurchaseRequest(prId);

            assertThat(testPr.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
            ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
            verify(approvalHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getApprovalLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("ปฏิเสธ PR ที่เป็น DRAFT ไม่ได้")
        void rejectDraft_throws() {
            testPr.setStatus(PurchaseRequestStatus.DRAFT);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.rejectPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
            verify(purchaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("ปฏิเสธ PR ที่ APPROVED ไปแล้วไม่ได้")
        void rejectApproved_throws() {
            testPr.setStatus(PurchaseRequestStatus.APPROVED);
            stubUntilGuard(approver);

            assertThatThrownBy(() -> purchaseRequestService.rejectPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ปฏิเสธ PR ที่ REJECTED ไปแล้วซ้ำไม่ได้")
        void rejectAlreadyRejected_throws() {
            testPr.setStatus(PurchaseRequestStatus.REJECTED);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.rejectPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("MANAGER ปฏิเสธ MANAGER_APPROVED ไม่ได้ (เป็นขั้นของ APPROVER)")
        void managerRejectsManagerApproved_throws() {
            testPr.setStatus(PurchaseRequestStatus.MANAGER_APPROVED);
            stubUntilGuard(manager);

            assertThatThrownBy(() -> purchaseRequestService.rejectPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("APPROVER ปฏิเสธ SUBMITTED ไม่ได้ (ต้องรอ MANAGER ก่อน)")
        void approverRejectsSubmitted_throws() {
            testPr.setStatus(PurchaseRequestStatus.SUBMITTED);
            stubUntilGuard(approver);

            assertThatThrownBy(() -> purchaseRequestService.rejectPurchaseRequest(prId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
