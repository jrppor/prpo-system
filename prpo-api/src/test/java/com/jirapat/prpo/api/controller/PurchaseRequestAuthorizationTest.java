package com.jirapat.prpo.api.controller;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import com.jirapat.prpo.api.dto.request.CreatePurchaseOrderRequest;


@DisplayName("PurchaseRequestController — authorization")
class PurchaseRequestAuthorizationTest {

    private static Method method(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "ไม่พบ method " + type.getSimpleName() + "#" + name
                            + " — signature อาจเปลี่ยนไป โปรดอัปเดตเทส", e);
        }
    }

    @Nested
    @DisplayName("endpoint อนุมัติ/ปฏิเสธ ต้องจำกัดด้วย role")
    class ApprovalEndpointsRestricted {

        @Test
        @DisplayName("/approve มี @PreAuthorize จำกัดเฉพาะ MANAGER/APPROVER")
        void approveEndpoint_isRestrictedToManagerAndApprover() {
            Method approve = method(PurchaseRequestController.class,
                    "approvePurchaseRequest", UUID.class);

            PreAuthorize ann = approve.getAnnotation(PreAuthorize.class);
            assertThat(ann)
                    .as("/{id}/approve ต้องมี @PreAuthorize")
                    .isNotNull();
            assertThat(ann.value()).contains("MANAGER", "APPROVER");
        }

        @Test
        @DisplayName("/reject มี @PreAuthorize จำกัดเฉพาะ MANAGER/APPROVER")
        void rejectEndpoint_isRestrictedToManagerAndApprover() {
            Method reject = method(PurchaseRequestController.class,
                    "rejectPurchaseRequest", UUID.class);

            PreAuthorize ann = reject.getAnnotation(PreAuthorize.class);
            assertThat(ann)
                    .as("/{id}/reject ต้องมี @PreAuthorize")
                    .isNotNull();
            assertThat(ann.value()).contains("MANAGER", "APPROVER");
        }
    }

    @Nested
    @DisplayName("จุดอื่นที่ควรจำกัดสิทธิ์อยู่แล้ว (regression guard)")
    class OtherRestrictedEndpoints {

        @Test
        @DisplayName("/pending-approval ของ PR controller มี @PreAuthorize (MANAGER/APPROVER)")
        void pendingApprovalEndpoint_isRoleRestricted() {
            Method pending = method(PurchaseRequestController.class,
                    "getAllPendingApproval", Pageable.class);

            PreAuthorize ann = pending.getAnnotation(PreAuthorize.class);
            assertThat(ann).isNotNull();
            assertThat(ann.value()).contains("MANAGER", "APPROVER");
        }

        @Test
        @DisplayName("createPurchaseOrder ของ PO controller มี @PreAuthorize (PROCUREMENT/ADMIN)")
        void poCreateEndpoint_isRoleRestricted() {
            Method poCreate = method(PurchaseOrderController.class,
                    "createPurchaseOrder", CreatePurchaseOrderRequest.class);

            PreAuthorize ann = poCreate.getAnnotation(PreAuthorize.class);
            assertThat(ann).isNotNull();
            assertThat(ann.value()).contains("PROCUREMENT", "ADMIN");
        }
    }
}
