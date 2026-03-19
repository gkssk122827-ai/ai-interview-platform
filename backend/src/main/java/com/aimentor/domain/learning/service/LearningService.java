package com.aimentor.domain.learning.service;

import com.aimentor.domain.learning.dto.request.LearningGenerateRequest;
import com.aimentor.domain.learning.dto.request.LearningGradeRequest;
import com.aimentor.domain.learning.dto.response.LearningGenerateResponse;
import com.aimentor.domain.learning.dto.response.LearningGradeResponse;
import com.aimentor.domain.learning.dto.response.LearningProblemResponse;
import com.aimentor.external.ai.AiService;
import org.springframework.stereotype.Service;

@Service
public class LearningService {

    private final AiService aiService;

    public LearningService(AiService aiService) {
        this.aiService = aiService;
    }

    public LearningGenerateResponse generateProblems(LearningGenerateRequest request) {
        return new LearningGenerateResponse(
                aiService.generateLearningProblems(request.subject(), request.difficulty(), request.count(), "MULTIPLE").stream()
                        .map(problem -> new LearningProblemResponse(
                                problem.type(),
                                problem.question(),
                                problem.choices(),
                                problem.answer(),
                                problem.explanation()
                        ))
                        .toList()
        );
    }

    public LearningGradeResponse gradeAnswer(LearningGradeRequest request) {
        var result = aiService.gradeLearningAnswer(
                request.question(),
                request.correctAnswer(),
                request.userAnswer(),
                request.explanation()
        );
        return new LearningGradeResponse(result.isCorrect(), result.aiFeedback());
    }
}
