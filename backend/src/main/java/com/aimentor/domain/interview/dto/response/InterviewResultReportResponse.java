package com.aimentor.domain.interview.dto.response;

import com.aimentor.domain.interview.entity.InterviewSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Returns a session report including all QA items and feedback.
 */
public record InterviewResultReportResponse(
        Long sessionId,
        Long userId,
        Long applicationDocumentId,
        Long resumeId,
        Long coverLetterId,
        Long jobPostingId,
        String title,
        String positionTitle,
        InterviewSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer totalQuestions,
        Integer answeredQuestions,
        Integer unansweredQuestions,
        Integer completionRate,
        Boolean completed,
        Long durationMinutes,
        String summary,
        List<String> highlights,
        List<InterviewLearningRecommendationResponse> learningRecommendations,
        List<InterviewQuestionResponse> questions,
        InterviewFeedbackResponse feedback
) {
}
