package com.jirapat.prpo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.dto.request.LoginRequest;
import com.jirapat.prpo.dto.request.RefreshTokenRequest;
import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.AuthResponse;
import com.jirapat.prpo.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs สำหรับเข้าสู่ระบบ")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "เข้าสู่ระบบ", description = "เข้าสู่ระบบด้วย email และ password เพื่อรับ JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "ต่ออายุ token", description = "ใช้ refresh token เพื่อขอ access token ใหม่")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }
}


