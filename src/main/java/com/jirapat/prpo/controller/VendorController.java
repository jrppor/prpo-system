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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.dto.request.CreateVendorRequest;
import com.jirapat.prpo.dto.request.UpdateVendorRequest;
import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.VendorResponse;
import com.jirapat.prpo.service.VendorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "APIs สำหรับ vendor")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VendorResponse>>> getAllVendors (
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String taxId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<VendorResponse> response = vendorService.getAllVendors(
            code,
            vendorName,
            taxId,
            pageable
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendorById(@PathVariable UUID id) {

        VendorResponse response = vendorService.getVendorById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        VendorResponse response = vendorService.createVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vendor created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "แก้ไข vendor", description = "อัพเดทข้อมูล vendor (เฉพาะ Admin)")
    public ResponseEntity<ApiResponse<VendorResponse>> updateVendor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorRequest request) {
        VendorResponse response = vendorService.updateVendor(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vendor updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVendor(@PathVariable UUID id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok(ApiResponse.success("Vendor deleted successfully"));
    }
}
