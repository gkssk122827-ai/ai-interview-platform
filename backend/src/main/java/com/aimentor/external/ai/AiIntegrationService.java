package com.aimentor.external.ai;

import com.aimentor.external.ai.dto.AiAnalyzeAnswerFeedbackRequest;
import com.aimentor.external.ai.dto.AiAnalyzeAnswerFeedbackResponse;
import com.aimentor.external.ai.dto.AiGenerateInterviewQuestionsRequest;
import com.aimentor.external.ai.dto.AiGenerateInterviewQuestionsResponse;
import com.aimentor.external.ai.dto.AiGenerateReportSummaryRequest;
import com.aimentor.external.ai.dto.AiGenerateReportSummaryResponse;

/**
 * Defines the backend-facing contract for interview AI features.
 */
public interface AiIntegrationService {

    AiGenerateInterviewQuestionsResponse generateInterviewQuestions(AiGenerateInterviewQuestionsRequest request);

    AiAnalyzeAnswerFeedbackResponse analyzeAnswerFeedback(AiAnalyzeAnswerFeedbackRequest request);

    AiGenerateReportSummaryResponse generateReportSummary(AiGenerateReportSummaryRequest request);
}
