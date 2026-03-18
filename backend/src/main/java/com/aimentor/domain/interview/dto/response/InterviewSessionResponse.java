package com.aimentor.domain.interview.dto.response;

import com.aimentor.domain.interview.entity.InterviewMode;
import com.aimentor.domain.interview.entity.InterviewSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Returns an interview session with its QA items and optional feedback.
 */
public record InterviewSessionResponse(
        Long id,
        Long userId,
        Long applicationDocumentId,
        Long resumeId,
        Long coverLetterId,
        Long jobPostingId,
        String title,
        String positionTitle,
        InterviewMode mode,
        InterviewSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer totalQuestions,
        Integer answeredQuestions,
        Integer unansweredQuestions,
        Integer completionRate,
        List<InterviewQuestionResponse> questions,
        InterviewFeedbackResponse feedback,
        String questionGenerationSource,
        Boolean questionGenerationFallbackUsed,
        String questionGenerationMessage
) {
}
