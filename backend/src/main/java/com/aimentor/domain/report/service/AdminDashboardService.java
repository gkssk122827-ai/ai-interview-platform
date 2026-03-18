package com.aimentor.domain.report.service;

import com.aimentor.domain.interview.entity.InterviewSession;
import com.aimentor.domain.interview.entity.InterviewSessionStatus;
import com.aimentor.domain.interview.repository.InterviewSessionRepository;
import com.aimentor.domain.order.repository.OrderRepository;
import com.aimentor.domain.profile.entity.ApplicationDocument;
import com.aimentor.domain.profile.entity.JobPosting;
import com.aimentor.domain.profile.repository.ApplicationDocumentRepository;
import com.aimentor.domain.profile.repository.JobPostingRepository;
import com.aimentor.domain.report.dto.response.AdminDashboardDailySignupResponse;
import com.aimentor.domain.report.dto.response.AdminDashboardResponse;
import com.aimentor.domain.report.dto.response.AdminRecentApplicationDocumentResponse;
import com.aimentor.domain.report.dto.response.AdminRecentInterviewSessionResponse;
import com.aimentor.domain.report.dto.response.AdminRecentUserResponse;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_LIMIT = 5;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final JobPostingRepository jobPostingRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            InterviewSessionRepository interviewSessionRepository,
            ApplicationDocumentRepository applicationDocumentRepository,
            JobPostingRepository jobPostingRepository
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.interviewSessionRepository = interviewSessionRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        List<User> users = userRepository.findAll();
        List<ApplicationDocument> applicationDocuments = applicationDocumentRepository.findAll();
        List<JobPosting> jobPostings = jobPostingRepository.findAll();
        List<InterviewSession> interviewSessions = interviewSessionRepository.findAll();

        long totalUsers = users.size();
        long totalApplicationDocuments = applicationDocuments.size();
        long totalJobPostings = jobPostings.size();
        long ongoingInterviews = interviewSessions.stream()
                .filter(session -> session.getStatus() == InterviewSessionStatus.ONGOING)
                .count();

        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .map(order -> order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AdminDashboardDailySignupResponse> dailySignups = startDate.datesUntil(today.plusDays(1))
                .map(date -> new AdminDashboardDailySignupResponse(
                        date.toString(),
                        users.stream()
                                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().toLocalDate().isEqual(date))
                                .count()
                ))
                .toList();

        Comparator<LocalDateTime> dateTimeComparator = Comparator.nullsLast(LocalDateTime::compareTo);

        List<AdminRecentUserResponse> recentUsers = users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, dateTimeComparator).reversed())
                .limit(RECENT_LIMIT)
                .map(user -> new AdminRecentUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getCreatedAt()
                ))
                .toList();

        List<AdminRecentApplicationDocumentResponse> recentApplicationDocuments = applicationDocuments.stream()
                .sorted(Comparator.comparing(ApplicationDocument::getCreatedAt, dateTimeComparator).reversed())
                .limit(RECENT_LIMIT)
                .map(document -> new AdminRecentApplicationDocumentResponse(
                        document.getId(),
                        document.getUser().getId(),
                        document.getTitle(),
                        document.getUser().getName(),
                        document.getUser().getEmail(),
                        document.getCreatedAt()
                ))
                .toList();

        List<AdminRecentInterviewSessionResponse> recentInterviewSessions = interviewSessions.stream()
                .sorted(Comparator.comparing(InterviewSession::getCreatedAt, dateTimeComparator).reversed())
                .limit(RECENT_LIMIT)
                .map(session -> new AdminRecentInterviewSessionResponse(
                        session.getId(),
                        session.getUser().getId(),
                        session.getUser().getName(),
                        session.getUser().getEmail(),
                        session.getTitle(),
                        session.getPositionTitle(),
                        session.getStatus().name(),
                        session.getStartedAt(),
                        session.getCreatedAt()
                ))
                .toList();

        return new AdminDashboardResponse(
                totalUsers,
                totalApplicationDocuments,
                totalJobPostings,
                ongoingInterviews,
                totalOrders,
                totalRevenue,
                dailySignups,
                recentUsers,
                recentApplicationDocuments,
                recentInterviewSessions
        );
    }
}
