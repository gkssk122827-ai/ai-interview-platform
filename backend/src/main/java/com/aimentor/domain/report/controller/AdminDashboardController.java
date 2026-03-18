package com.aimentor.domain.report.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.domain.report.dto.response.AdminDashboardResponse;
import com.aimentor.domain.report.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminDashboardService.getDashboard());
    }
}
