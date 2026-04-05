package com.jirapat.prpo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.DashboardSummaryResponse;
import com.jirapat.prpo.dto.response.MonthlySpendingResponse;
import com.jirapat.prpo.dto.response.TopVendorResponse;
import com.jirapat.prpo.service.DashboardService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs สำหรับ Dashboard & Reporting")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse response = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<MonthlySpendingResponse>>> getMonthlySpending(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<MonthlySpendingResponse> response = dashboardService.getMonthlySpending(targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-vendors")
    public ResponseEntity<ApiResponse<List<TopVendorResponse>>> getTopVendors(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopVendorResponse> response = dashboardService.getTopVendors(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
