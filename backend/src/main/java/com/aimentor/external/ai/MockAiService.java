package com.aimentor.external.ai;

import com.aimentor.external.ai.dto.FeedbackDto;
import com.aimentor.external.ai.dto.GradeResultDto;
import com.aimentor.external.ai.dto.ProblemDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Provides deterministic mock AI results for local development and testing.
 */
@Service
@ConditionalOnProperty(prefix = "integration.ai", name = "provider", havingValue = "mock-ai", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public String generateInterviewQuestion(
            String resumeContent,
            String coverLetterContent,
            String jobDescription,
            List<ConversationTurnDto> history
    ) {
        int nextQuestionNumber = extractQuestionNumber(jobDescription);
        return "Mock interview question " + nextQuestionNumber + " based on the provided profile context.";
    }

    @Override
    public FeedbackDto generateFeedback(List<ConversationTurnDto> history) {
        int answeredCount = history == null ? 0 : history.size();
        int overallScore = Math.min(100, 60 + answeredCount * 5);
        return new FeedbackDto(
                Math.min(100, overallScore + 3),
                Math.max(0, overallScore - 2),
                Math.min(100, overallScore + 1),
                overallScore,
                "Mock weak points: expand concrete evidence in some answers.",
                "Mock improvements: structure each answer with situation, action, and result.",
                "Mock recommended answer: explain your contribution and quantify the outcome."
        );
    }

    @Override
    public List<ProblemDto> generateLearningProblems(String subject, String difficulty, int count, String type) {
        int problemCount = Math.max(1, count);
        List<ProblemDto> problems = new ArrayList<>();
        for (int index = 1; index <= problemCount; index++) {
            boolean multiple = !"SHORT".equalsIgnoreCase(type) && ("MULTIPLE".equalsIgnoreCase(type) || "MIX".equalsIgnoreCase(type) ? index % 2 == 1 : true);
            problems.add(new ProblemDto(
                    multiple ? "MULTIPLE" : "SHORT",
                    "Mock " + subject + " question " + index,
                    multiple ? List.of("Option A", "Option B", "Option C", "Option D") : null,
                    multiple ? "Option A" : "Mock short answer " + index,
                    "Mock explanation for " + difficulty + " difficulty."
            ));
        }
        return problems;
    }

    @Override
    public GradeResultDto gradeLearningAnswer(String question, String correctAnswer, String userAnswer, String explanation) {
        boolean correct = correctAnswer != null && correctAnswer.equalsIgnoreCase(userAnswer == null ? "" : userAnswer.trim());
        return new GradeResultDto(
                correct,
                correct
                        ? "Correct. You understood the core concept."
                        : "Incorrect. Review the explanation and organize the core concept again."
        );
    }

    private int extractQuestionNumber(String jobDescription) {
        if (jobDescription == null) {
            return 1;
        }

        String marker = "Planned question number:";
        int markerIndex = jobDescription.indexOf(marker);
        if (markerIndex < 0) {
            return 1;
        }

        String remainder = jobDescription.substring(markerIndex + marker.length()).trim();
        StringBuilder digits = new StringBuilder();
        for (char value : remainder.toCharArray()) {
            if (Character.isDigit(value)) {
                digits.append(value);
                continue;
            }
            if (!digits.isEmpty()) {
                break;
            }
        }

        if (digits.isEmpty()) {
            return 1;
        }

        return Integer.parseInt(digits.toString());
    }
}
