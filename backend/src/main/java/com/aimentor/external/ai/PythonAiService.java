package com.aimentor.external.ai;

import com.aimentor.external.ai.dto.FeedbackDto;
import com.aimentor.external.ai.dto.GradeResultDto;
import com.aimentor.external.ai.dto.InterviewQuestionGenerationContext;
import com.aimentor.external.ai.dto.ProblemDto;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the external Python AI server for interview and learning features.
 */
@Service
@ConditionalOnProperty(prefix = "integration.ai", name = "provider", havingValue = "python")
public class PythonAiService implements AiService {

    private final RestTemplate restTemplate;
    private final AiServerProperties aiServerProperties;

    public PythonAiService(AiServerProperties aiServerProperties) {
        this.restTemplate = new RestTemplate();
        this.aiServerProperties = aiServerProperties;
    }

    @Override
    public String generateInterviewQuestion(
            String resumeContent,
            String coverLetterContent,
            String jobDescription,
            InterviewQuestionGenerationContext context,
            List<ConversationTurnDto> history
    ) {
        try {
            ResponseEntity<InterviewQuestionResponse> response = restTemplate.exchange(
                    aiServerProperties.url() + "/interview/question",
                    HttpMethod.POST,
                    new HttpEntity<>(new InterviewQuestionRequest(
                            resumeContent,
                            coverLetterContent,
                            jobDescription,
                            context.interviewMode(),
                            context.positionCategory(),
                            context.questionDifficulty(),
                            context.questionIndex(),
                            context.totalQuestionCount(),
                            context.modeGuide(),
                            context.existingQuestions(),
                            history
                    )),
                    InterviewQuestionResponse.class
            );
            return response.getBody() == null ? null : response.getBody().question();
        } catch (RestClientException ex) {
            throw new AiServiceException("Failed to generate interview question from Python AI server.", ex);
        }
    }

    @Override
    public FeedbackDto generateFeedback(List<ConversationTurnDto> history) {
        try {
            ResponseEntity<FeedbackDto> response = restTemplate.exchange(
                    aiServerProperties.url() + "/interview/feedback",
                    HttpMethod.POST,
                    new HttpEntity<>(new FeedbackRequest(history)),
                    FeedbackDto.class
            );
            return response.getBody();
        } catch (RestClientException ex) {
            throw new AiServiceException("Failed to generate interview feedback from Python AI server.", ex);
        }
    }

    @Override
    public List<ProblemDto> generateLearningProblems(String subject, String difficulty, int count, String type) {
        try {
            ResponseEntity<LearningGenerateResponse> response = restTemplate.exchange(
                    aiServerProperties.url() + "/learning/generate",
                    HttpMethod.POST,
                    new HttpEntity<>(new LearningProblemRequest(subject, difficulty, count, type)),
                    LearningGenerateResponse.class
            );
            LearningGenerateResponse body = response.getBody();
            return body == null || body.problems() == null ? List.of() : body.problems();
        } catch (RestClientException ex) {
            throw new AiServiceException("Failed to generate learning problems from Python AI server.", ex);
        }
    }

    @Override
    public GradeResultDto gradeLearningAnswer(String question, String correctAnswer, String userAnswer, String explanation) {
        try {
            ResponseEntity<GradeResultDto> response = restTemplate.exchange(
                    aiServerProperties.url() + "/learning/grade",
                    HttpMethod.POST,
                    new HttpEntity<>(new GradeRequest(question, correctAnswer, userAnswer, explanation)),
                    GradeResultDto.class
            );
            return response.getBody();
        } catch (RestClientException ex) {
            throw new AiServiceException("Failed to grade learning answer from Python AI server.", ex);
        }
    }

    /**
     * Maps the backend interview-question request to the Python AI server contract.
     */
    private record InterviewQuestionRequest(
            String resumeContent,
            String coverLetterContent,
            String jobDescription,
            String interviewMode,
            String positionCategory,
            String questionDifficulty,
            int questionIndex,
            int totalQuestionCount,
            String modeGuide,
            List<String> existingQuestions,
            List<ConversationTurnDto> conversationHistory
    ) {
    }

    /**
     * Maps the backend feedback request to the Python AI server contract.
     */
    private record FeedbackRequest(
            List<ConversationTurnDto> conversationHistory
    ) {
    }

    /**
     * Maps the backend learning-problem request to the Python AI server contract.
     */
    private record LearningProblemRequest(
            String subject,
            String difficulty,
            int count,
            String type
    ) {
    }

    /**
     * Maps the backend grading request to the Python AI server contract.
     */
    private record GradeRequest(
            String question,
            String correctAnswer,
            String userAnswer,
            String explanation
    ) {
    }

    private record LearningGenerateResponse(
            List<ProblemDto> problems
    ) {
    }

    /**
     * Reads the Python AI server interview-question response body.
     */
    private record InterviewQuestionResponse(
            String question
    ) {
    }
}
