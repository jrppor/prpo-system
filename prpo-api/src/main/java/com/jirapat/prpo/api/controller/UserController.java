package com.jirapat.prpo.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.api.dto.request.CreateUserRequest;
import com.jirapat.prpo.api.dto.request.UpdateActiveRequest;
import com.jirapat.prpo.api.dto.request.UpdateProfileRequest;
import com.jirapat.prpo.api.dto.request.UpdateRoleRequest;
import com.jirapat.prpo.api.dto.response.ApiResponse;
import com.jirapat.prpo.api.dto.response.UserResponse;
import com.jirapat.prpo.api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs สำหรับจัดการผู้ใช้งาน")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ============ Profile Endpoints ============

    @GetMapping("/me")
    @Operation(summary = "ดูโปรไฟล์ตัวเอง", description = "ดึงข้อมูลโปรไฟล์ของผู้ใช้ที่ล็อกอินอยู่")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        UserResponse response = userService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "แก้ไขโปรไฟล์ตัวเอง", description = "อัพเดทชื่อ-นามสกุลของผู้ใช้ที่ล็อกอินอยู่")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    // ============ Admin Endpoints ============

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "สร้างผู้ใช้ใหม่", description = "สร้างบัญชีผู้ใช้ใหม่พร้อมกำหนด role (เฉพาะ Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ดูรายชื่อผู้ใช้ทั้งหมด", description = "ดึงรายชื่อผู้ใช้ทั้งหมด (เฉพาะ Admin)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> response = userService.getAllUsers(email, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ดูข้อมูลผู้ใช้ตาม ID", description = "ดึงข้อมูลผู้ใช้ตาม ID (เฉพาะ Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "เปลี่ยน role ผู้ใช้", description = "เปลี่ยน role ของผู้ใช้ (เฉพาะ Admin, ไม่สามารถเปลี่ยน role ตัวเอง)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UserResponse response = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "User role updated successfully"));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "เปิด/ปิดการใช้งานผู้ใช้", description = "Activate หรือ Deactivate ผู้ใช้ (เฉพาะ Admin, ไม่สามารถ deactivate ตัวเอง)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserActive(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateActiveRequest request) {
        UserResponse response = userService.updateUserActive(id, request);
        String message = request.getIsActive() ? "User activated successfully" : "User deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
