package com.aimentor.domain.profile.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.profile.dto.request.JobPostingUpsertRequest;
import com.aimentor.domain.profile.dto.response.JobPostingResponse;
import com.aimentor.domain.profile.dto.response.JobPostingUrlPreviewResponse;
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
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final JobPostingUrlMetadataService jobPostingUrlMetadataService;

    public JobPostingService(
            JobPostingRepository jobPostingRepository,
            UserRepository userRepository,
            JobPostingUrlMetadataService jobPostingUrlMetadataService
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.jobPostingUrlMetadataService = jobPostingUrlMetadataService;
    }

    @Transactional
    public JobPostingResponse create(Role role, Long userId, JobPostingUpsertRequest request) {
        User user = getUser(userId);
        JobPosting jobPosting = JobPosting.builder()
                .user(user)
                .companyName(request.companyName())
                .positionTitle(request.positionTitle())
                .description(request.description())
                .fileUrl(request.fileUrl())
                .jobUrl(request.jobUrl())
                .deadline(request.deadline())
                .siteName(resolveSiteName(request))
                .sourceStatus(resolveSourceStatus(request))
                .build();
        return toResponse(jobPostingRepository.save(jobPosting));
    }

    public List<JobPostingResponse> list(Role role, Long userId, String keyword) {
        List<JobPosting> jobPostings;
        if (Objects.equals(role, Role.ADMIN)) {
            jobPostings = keyword == null || keyword.isBlank()
                    ? jobPostingRepository.findAllByOrderByUpdatedAtDesc()
                    : jobPostingRepository.findByPositionTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCaseOrderByUpdatedAtDesc(keyword, keyword);
        } else {
            jobPostings = keyword == null || keyword.isBlank()
                    ? jobPostingRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                    : jobPostingRepository.findByUserIdAndPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, keyword);
        }
        return jobPostings.stream().map(this::toResponse).toList();
    }

    public JobPostingResponse get(Role role, Long userId, Long jobPostingId) {
        return toResponse(getJobPosting(role, userId, jobPostingId));
    }

    @Transactional
    public JobPostingResponse update(Role role, Long userId, Long jobPostingId, JobPostingUpsertRequest request) {
        JobPosting jobPosting = getJobPosting(role, userId, jobPostingId);
        jobPosting.update(
                request.companyName(),
                request.positionTitle(),
                request.description(),
                request.fileUrl(),
                request.jobUrl(),
                request.deadline(),
                resolveSiteName(request),
                resolveSourceStatus(request)
        );
        return toResponse(jobPosting);
    }

    @Transactional
    public void delete(Role role, Long userId, Long jobPostingId) {
        jobPostingRepository.delete(getJobPosting(role, userId, jobPostingId));
    }

    public JobPostingUrlPreviewResponse previewUrl(String url) {
        validateUrl(url);
        return jobPostingUrlMetadataService.preview(url);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "회원 정보를 찾을 수 없습니다."));
    }

    private JobPosting getJobPosting(Role role, Long userId, Long jobPostingId) {
        if (Objects.equals(role, Role.ADMIN)) {
            return jobPostingRepository.findById(jobPostingId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_POSTING_NOT_FOUND", "채용공고를 찾을 수 없습니다."));
        }
        return jobPostingRepository.findByIdAndUserId(jobPostingId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_POSTING_NOT_FOUND", "채용공고를 찾을 수 없습니다."));
    }

    private void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "JOB_POSTING_URL_REQUIRED", "채용공고 URL을 입력해 주세요.");
        }
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("invalid scheme");
            }
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "JOB_POSTING_URL_INVALID", "올바른 채용공고 URL이 아닙니다.");
        }
    }

    private String resolveSiteName(JobPostingUpsertRequest request) {
        if (StringUtils.hasText(request.siteName())) {
            return request.siteName().trim();
        }
        if (StringUtils.hasText(request.jobUrl())) {
            try {
                return jobPostingUrlMetadataService.resolveSiteName(java.net.URI.create(request.jobUrl().trim()).getHost());
            } catch (Exception ignored) {
                return "기타";
            }
        }
        return "수동입력";
    }

    private String resolveSourceStatus(JobPostingUpsertRequest request) {
        return StringUtils.hasText(request.jobUrl()) ? "URL_REGISTERED" : "MANUAL";
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
                jobPosting.getSiteName(),
                jobPosting.getSourceStatus(),
                jobPosting.getDeadline(),
                jobPosting.getCreatedAt(),
                jobPosting.getUpdatedAt()
        );
    }
}
