package com.aimentor.domain.profile.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.profile.dto.request.JobPostingUpsertRequest;
import com.aimentor.domain.profile.dto.response.JobPostingResponse;
import com.aimentor.domain.profile.entity.JobPosting;
import com.aimentor.domain.profile.repository.JobPostingRepository;
import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles shared job-posting reads for users and management operations for admins.
 */
@Service
@Transactional(readOnly = true)
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    public JobPostingService(JobPostingRepository jobPostingRepository, UserRepository userRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JobPostingResponse create(Role role, Long userId, JobPostingUpsertRequest request) {
        ensureAdmin(role);
        User user = getUser(userId);
        JobPosting jobPosting = JobPosting.builder()
                .user(user)
                .companyName(request.companyName())
                .positionTitle(request.positionTitle())
                .description(request.description())
                .fileUrl(request.fileUrl())
                .jobUrl(request.jobUrl())
                .deadline(request.deadline())
                .build();
        return toResponse(jobPostingRepository.save(jobPosting));
    }

    public List<JobPostingResponse> list(String keyword) {
        List<JobPosting> jobPostings = keyword == null || keyword.isBlank()
                ? jobPostingRepository.findAllByOrderByUpdatedAtDesc()
                : jobPostingRepository.findByPositionTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCaseOrderByUpdatedAtDesc(keyword, keyword);
        return jobPostings.stream().map(this::toResponse).toList();
    }

    public JobPostingResponse get(Long jobPostingId) {
        return toResponse(getJobPosting(jobPostingId));
    }

    @Transactional
    public JobPostingResponse update(Role role, Long jobPostingId, JobPostingUpsertRequest request) {
        ensureAdmin(role);
        JobPosting jobPosting = getJobPosting(jobPostingId);
        jobPosting.update(
                request.companyName(),
                request.positionTitle(),
                request.description(),
                request.fileUrl(),
                request.jobUrl(),
                request.deadline()
        );
        return toResponse(jobPosting);
    }

    @Transactional
    public void delete(Role role, Long jobPostingId) {
        ensureAdmin(role);
        jobPostingRepository.delete(getJobPosting(jobPostingId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private JobPosting getJobPosting(Long jobPostingId) {
        return jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_POSTING_NOT_FOUND", "Job posting not found."));
    }

    private void ensureAdmin(Role role) {
        if (!Objects.equals(role, Role.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Admin permission is required.");
        }
    }

    private JobPostingResponse toResponse(JobPosting jobPosting) {
        return new JobPostingResponse(
                jobPosting.getId(),
                jobPosting.getUser().getId(),
                jobPosting.getCompanyName(),
                jobPosting.getPositionTitle(),
                jobPosting.getDescription(),
                jobPosting.getFileUrl(),
                jobPosting.getJobUrl(),
                jobPosting.getDeadline(),
                jobPosting.getCreatedAt(),
                jobPosting.getUpdatedAt()
        );
    }
}
