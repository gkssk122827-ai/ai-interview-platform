package com.aimentor.domain.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalApplicationDocuments,
        long totalJobPostings,
        long ongoingInterviews,
        long totalOrders,
        BigDecimal totalRevenue,
        List<AdminDashboardDailySignupResponse> dailySignups,
        List<AdminRecentUserResponse> recentUsers,
        List<AdminRecentApplicationDocumentResponse> recentApplicationDocuments,
        List<AdminRecentInterviewSessionResponse> recentInterviewSessions
) {
}
