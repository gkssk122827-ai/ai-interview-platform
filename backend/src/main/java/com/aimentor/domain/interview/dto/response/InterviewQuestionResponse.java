package com.aimentor.domain.interview.dto.response;

import java.time.LocalDateTime;

/**
 * Returns a question and its saved answer as one QA item.
 */
public record InterviewQuestionResponse(
        Long id,
        Integer sequenceNumber,
        String questionText,
        String answerText,
        String audioUrl,
        Boolean answered,
        Integer answerLength,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
}
