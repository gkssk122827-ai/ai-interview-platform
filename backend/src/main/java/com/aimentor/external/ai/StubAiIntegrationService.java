package com.aimentor.external.ai;

import com.aimentor.external.ai.dto.AiAnalyzeAnswerFeedbackRequest;
import com.aimentor.external.ai.dto.AiAnalyzeAnswerFeedbackResponse;
import com.aimentor.external.ai.dto.AiGenerateInterviewQuestionsRequest;
import com.aimentor.external.ai.dto.AiGenerateInterviewQuestionsResponse;
import com.aimentor.external.ai.dto.AiGenerateReportSummaryRequest;
import com.aimentor.external.ai.dto.AiGenerateReportSummaryResponse;
import com.aimentor.external.ai.dto.AiQuestionItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StubAiIntegrationService implements AiIntegrationService {

    private final AiIntegrationProperties properties;

    public StubAiIntegrationService(AiIntegrationProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiGenerateInterviewQuestionsResponse generateInterviewQuestions(AiGenerateInterviewQuestionsRequest request) {
        List<AiQuestionItem> questions = new ArrayList<>();
        int questionCount = Math.max(1, request.questionCount());
        for (int index = 1; index <= questionCount; index++) {
            questions.add(new AiQuestionItem(
                    index,
                    "Stub question " + index + " for " + request.positionTitle() + ". Replace with real AI provider output."
            ));
        }

        // Real provider integration will call the selected AI SDK or HTTP API here.
        return new AiGenerateInterviewQuestionsResponse(questions, resolveProviderName(), true);
    }

    @Override
    public AiAnalyzeAnswerFeedbackResponse analyzeAnswerFeedback(AiAnalyzeAnswerFeedbackRequest request) {
        int answerLength = request.answerText() == null ? 0 : request.answerText().trim().length();
        int relevanceScore = Math.min(100, 50 + (answerLength / 20));
        int logicScore = Math.min(100, 45 + (answerLength / 25));
        int specificityScore = Math.min(100, 40 + (answerLength / 18));
        int overallScore = Math.round((relevanceScore + logicScore + specificityScore) / 3.0f);

        // Real provider integration will send prompt/context and parse structured scoring here.
        return new AiAnalyzeAnswerFeedbackResponse(
                relevanceScore,
                logicScore,
                specificityScore,
                overallScore,
                "Stub feedback generated from a simple local heuristic. Replace with provider analysis.",
                resolveProviderName(),
                true
        );
    }

    @Override
    public AiGenerateReportSummaryResponse generateReportSummary(AiGenerateReportSummaryRequest request) {
        int feedbackCount = request.answerFeedback() == null ? 0 : request.answerFeedback().size();

        // Real provider integration will summarize answer-level feedback into a session report here.
        return new AiGenerateReportSummaryResponse(
                "Stub report summary for session '" + request.sessionTitle()
                        + "' with " + feedbackCount + " analyzed answers. Replace with provider-generated summary.",
                resolveProviderName(),
                true
        );
    }

    private String resolveProviderName() {
        return properties.provider() == null || properties.provider().isBlank()
                ? "stub-ai"
                : properties.provider();
    }
}
