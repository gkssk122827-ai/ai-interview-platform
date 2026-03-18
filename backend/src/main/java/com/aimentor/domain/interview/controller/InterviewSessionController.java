package com.aimentor.domain.interview.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.interview.dto.request.SaveInterviewAnswerRequest;
import com.aimentor.domain.interview.dto.request.StartInterviewSessionRequest;
import com.aimentor.domain.interview.dto.response.InterviewAnswerResponse;
import com.aimentor.domain.interview.dto.response.InterviewFeedbackResponse;
import com.aimentor.domain.interview.dto.response.InterviewResultReportResponse;
import com.aimentor.domain.interview.dto.response.InterviewSessionResponse;
import com.aimentor.domain.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes interview-session APIs for authenticated users.
 */
@RestController
@RequestMapping({"/api/interviews/sessions", "/api/v1/interviews/sessions"})
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    public InterviewSessionController(InterviewSessionService interviewSessionService) {
        this.interviewSessionService = interviewSessionService;
    }

    @PostMapping
    public ApiResponse<InterviewSessionResponse> startSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody StartInterviewSessionRequest request
    ) {
        return ApiResponse.success(interviewSessionService.startSession(authenticatedUser.userId(), request));
    }

    @GetMapping
    public ApiResponse<List<InterviewSessionResponse>> listSessions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.success(interviewSessionService.listSessions(authenticatedUser.userId()));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<InterviewSessionResponse> getSessionDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(interviewSessionService.getSessionDetail(authenticatedUser.userId(), sessionId));
    }

    @PostMapping({"/{sessionId}/answer", "/{sessionId}/answers"})
    public ApiResponse<InterviewAnswerResponse> saveAnswer(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId,
            @Valid @RequestBody SaveInterviewAnswerRequest request
    ) {
        return ApiResponse.success(interviewSessionService.saveAnswer(authenticatedUser.userId(), sessionId, request));
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponse<InterviewSessionResponse> endSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(interviewSessionService.endSession(authenticatedUser.userId(), sessionId));
    }

    @GetMapping({"/{sessionId}/feedback", "/{sessionId}/report"})
    public ApiResponse<InterviewFeedbackResponse> getFeedback(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(interviewSessionService.getFeedback(authenticatedUser.userId(), sessionId));
    }

    @GetMapping("/{sessionId}/full-report")
    public ApiResponse<InterviewResultReportResponse> getResultReport(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(interviewSessionService.getResultReport(authenticatedUser.userId(), sessionId));
    }
}
