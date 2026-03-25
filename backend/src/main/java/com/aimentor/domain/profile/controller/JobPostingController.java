package com.aimentor.domain.profile.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.profile.dto.request.JobPostingUpsertRequest;
import com.aimentor.domain.profile.dto.request.JobPostingUrlRegisterRequest;
import com.aimentor.domain.profile.dto.response.JobPostingResponse;
import com.aimentor.domain.profile.dto.response.JobPostingUrlPreviewResponse;
import com.aimentor.domain.profile.service.JobPostingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles/job-postings")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @PostMapping
    public ApiResponse<JobPostingResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody JobPostingUpsertRequest request
    ) {
        return ApiResponse.success(jobPostingService.create(authenticatedUser.role(), authenticatedUser.userId(), request));
    }

    @GetMapping
    public ApiResponse<List<JobPostingResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(jobPostingService.list(authenticatedUser.role(), authenticatedUser.userId(), keyword));
    }

    @GetMapping("/{jobPostingId}")
    public ApiResponse<JobPostingResponse> get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long jobPostingId
    ) {
        return ApiResponse.success(jobPostingService.get(authenticatedUser.role(), authenticatedUser.userId(), jobPostingId));
    }

    @PutMapping("/{jobPostingId}")
    public ApiResponse<JobPostingResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long jobPostingId,
            @Valid @RequestBody JobPostingUpsertRequest request
    ) {
        return ApiResponse.success(jobPostingService.update(authenticatedUser.role(), authenticatedUser.userId(), jobPostingId, request));
    }

    @DeleteMapping("/{jobPostingId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long jobPostingId
    ) {
        jobPostingService.delete(authenticatedUser.role(), authenticatedUser.userId(), jobPostingId);
        return ApiResponse.success();
    }

    @PostMapping("/preview")
    public ApiResponse<JobPostingUrlPreviewResponse> previewUrl(@Valid @RequestBody JobPostingUrlRegisterRequest request) {
        return ApiResponse.success(jobPostingService.previewUrl(request.url()));
    }
}
