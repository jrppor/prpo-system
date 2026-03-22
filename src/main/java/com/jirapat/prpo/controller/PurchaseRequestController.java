package com.jirapat.prpo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.dto.request.CreatePurchaseRequestRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseRequestRequest;
import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.ApprovalHistoryResponse;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;
import com.jirapat.prpo.service.PurchaseRequestService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequests", description = "")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseRequestController {


    private final PurchaseRequestService purchaseRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseRequestResponse>>> getAllPurchaseRequests (
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PurchaseRequestResponse> response = purchaseRequestService.getAllPurchaseRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> getPurchaseRequestById(@PathVariable UUID id) {
        PurchaseRequestResponse response = purchaseRequestService.getPurchaseRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> createPurchaseRequest(
            @Valid @RequestBody CreatePurchaseRequestRequest request) {
        PurchaseRequestResponse response = purchaseRequestService.createPurchaseRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Purchase request created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> updatePurchaseRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseRequestRequest request) {
        PurchaseRequestResponse response = purchaseRequestService.updatePurchaseRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase request updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseRequest(@PathVariable UUID id) {
        purchaseRequestService.deletePurchaseRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase request deleted successfully"));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> submitPurchaseRequest(@PathVariable UUID id) {
        PurchaseRequestResponse response = purchaseRequestService.submitPurchaseRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase request submitted successfully"));
    }

    @GetMapping("/{id}/approval-history")
    public ResponseEntity<ApiResponse<List<ApprovalHistoryResponse>>> getApprovalHistory(@PathVariable UUID id) {
        List<ApprovalHistoryResponse> response = purchaseRequestService.getApprovalHistories(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> approvePurchaseRequest(@PathVariable UUID id) {
        PurchaseRequestResponse response = purchaseRequestService.approvePurchaseRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase request approved successfully"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> rejectPurchaseRequest(@PathVariable UUID id) {
        PurchaseRequestResponse response = purchaseRequestService.rejectPurchaseRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase request rejected successfully"));
    }


    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('MANAGER', 'APPROVER')")
    public ResponseEntity<ApiResponse<Page<PurchaseRequestResponse>>> getAllPendingApproval (
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PurchaseRequestResponse> response = purchaseRequestService.getPendingApproval(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
