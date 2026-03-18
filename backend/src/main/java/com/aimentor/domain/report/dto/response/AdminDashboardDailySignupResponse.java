package com.aimentor.domain.report.dto.response;

public record AdminDashboardDailySignupResponse(
        String date,
        long count
) {
}
