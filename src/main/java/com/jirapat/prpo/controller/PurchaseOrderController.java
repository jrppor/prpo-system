package com.jirapat.prpo.controller;


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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.dto.request.CreatePurchaseOrderRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseOrderRequest;
import com.jirapat.prpo.dto.request.UpdatePurchaseOrderStatusRequest;
import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.PurchaseOrderResponse;
import com.jirapat.prpo.service.PurchaseOrderService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "PurchaseOrders", description = "")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> getAllPurchaseOrders (
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        Page<PurchaseOrderResponse> response = purchaseOrderService.getAllPurchaseOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable UUID id) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok((ApiResponse.success(response)));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request
            ) {
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Purchase order created successfully"));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    @PostMapping("/from-pr/{prId}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrderFromPr(
            @PathVariable UUID prId,
            @RequestParam UUID vendorId
            ) {
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrderFromPr(prId, vendorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Purchase order created from PR successfully"));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseOrderRequest request
            ) {
        PurchaseOrderResponse response = purchaseOrderService.updatePurchaseOrder(id ,request);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase order updated successfully"));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseOrderStatusRequest request
            ) {
        PurchaseOrderResponse response = purchaseOrderService.updatePurchaseOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase order status updated successfully"));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable UUID id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order deleted successfully"));
    }

}
