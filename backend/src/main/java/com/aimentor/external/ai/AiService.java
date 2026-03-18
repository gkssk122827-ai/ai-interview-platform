package com.aimentor.external.ai;

import com.aimentor.external.ai.dto.FeedbackDto;
import com.aimentor.external.ai.dto.GradeResultDto;
import com.aimentor.external.ai.dto.ProblemDto;
import java.util.List;

/**
 * Defines the generic AI operations used by interview and learning domains.
 */
public interface AiService {

    String generateInterviewQuestion(
            String resumeContent,
            String coverLetterContent,
            String jobDescription,
            List<ConversationTurnDto> history
    );

    FeedbackDto generateFeedback(List<ConversationTurnDto> history);

    List<ProblemDto> generateLearningProblems(String subject, String difficulty, int count, String type);

    GradeResultDto gradeLearningAnswer(String question, String correctAnswer, String userAnswer, String explanation);
}
