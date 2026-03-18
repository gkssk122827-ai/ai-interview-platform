package com.aimentor.external.ai;

import com.aimentor.domain.interview.service.InterviewQuestionCatalog;
import com.aimentor.domain.interview.service.InterviewQuestionCatalog.InterviewQuestionCategory;
import com.aimentor.domain.interview.service.InterviewQuestionCatalog.InterviewQuestionDifficulty;
import com.aimentor.domain.interview.entity.InterviewMode;
import com.aimentor.external.ai.dto.FeedbackDto;
import com.aimentor.external.ai.dto.GradeResultDto;
import com.aimentor.external.ai.dto.InterviewQuestionGenerationContext;
import com.aimentor.external.ai.dto.ProblemDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
            InterviewQuestionGenerationContext context,
            List<ConversationTurnDto> history
    ) {
        InterviewMode mode = extractMode(context);
        InterviewQuestionCategory category = extractCategory(context);
        InterviewQuestionDifficulty difficulty = extractDifficulty(context);
        List<String> questions = InterviewQuestionCatalog.findQuestions(mode, category, difficulty);
        int nextQuestionNumber = Math.max(1, context.questionIndex());
        return questions.get(Math.min(nextQuestionNumber - 1, questions.size() - 1));
    }

    @Override
    public FeedbackDto generateFeedback(List<ConversationTurnDto> history) {
        int answeredCount = history == null ? 0 : history.size();
        if (answeredCount == 0) {
            return new FeedbackDto(
                    0,
                    0,
                    0,
                    0,
                    "아직 저장된 답변이 없어 결과를 분석할 수 없습니다.",
                    "최소 한 개 이상의 질문에 답변을 저장한 뒤 결과를 다시 확인해 보세요.",
                    "질문 의도를 먼저 설명하고 본인의 경험과 결과를 순서대로 정리해 보세요."
            );
        }

        int totalLength = 0;
        int structureHits = 0;
        int metricHits = 0;
        int keywordHits = 0;

        for (ConversationTurnDto turn : history) {
            String answer = turn.answer() == null ? "" : turn.answer().trim();
            totalLength += answer.length();
            structureHits += countMatches(answer, "상황", "문제", "목표", "과정", "행동", "결과", "배운", "느낀");
            metricHits += countMatches(answer, "%", "건", "명", "개월", "주", "배", "ms", "초", "성능", "지표");
            keywordHits += countMatches(answer, "사용", "구현", "개선", "설계", "협업", "테스트", "배포", "최적화", "해결");
        }

        int averageLength = totalLength / answeredCount;
        int logicScore = clampScore(45 + answeredCount * 7 + structureHits * 6 + averageLength / 22);
        int relevanceScore = clampScore(48 + answeredCount * 8 + keywordHits * 4 + averageLength / 18);
        int specificityScore = clampScore(40 + answeredCount * 7 + metricHits * 8 + averageLength / 16);
        int overallScore = Math.round((logicScore + relevanceScore + specificityScore) / 3.0f);

        return new FeedbackDto(
                logicScore,
                relevanceScore,
                specificityScore,
                overallScore,
                buildWeakPointsMessage(answeredCount, averageLength, structureHits, metricHits),
                buildImprovementsMessage(answeredCount, structureHits, metricHits, keywordHits, averageLength),
                buildRecommendedAnswerMessage(averageLength, metricHits)
        );
    }

    @Override
    public List<ProblemDto> generateLearningProblems(String subject, String difficulty, int count, String type) {
        int problemCount = Math.max(1, count);
        List<MockLearningTemplateCatalog.Template> templatePool = MockLearningTemplateCatalog.select(subject, difficulty);
        List<ProblemDto> problems = new ArrayList<>();
        for (int index = 1; index <= problemCount; index++) {
            boolean multiple = !"SHORT".equalsIgnoreCase(type)
                    && ("MULTIPLE".equalsIgnoreCase(type) || "MIX".equalsIgnoreCase(type)
                    ? index % 2 == 1
                    : true);
            MockLearningTemplateCatalog.Template template = templatePool.get((index - 1) % templatePool.size());
            problems.add(new ProblemDto(
                    multiple ? "MULTIPLE" : "SHORT",
                    template.question(),
                    multiple ? template.choices() : null,
                    multiple ? template.answer() : template.shortAnswer(),
                    template.explanation()
            ));
        }
        return problems;
    }

    @Override
    public GradeResultDto gradeLearningAnswer(String question, String correctAnswer, String userAnswer, String explanation) {
        String normalizedUserAnswer = userAnswer == null ? "" : userAnswer.trim();
        boolean correct = correctAnswer != null && correctAnswer.equalsIgnoreCase(normalizedUserAnswer);

        if (correct) {
            String feedback = StringUtils.hasText(explanation)
                    ? "정답입니다. " + explanation
                    : "정답입니다. 핵심 개념을 정확하게 이해하고 있습니다.";
            return new GradeResultDto(true, feedback);
        }

        String wrongExplanation = MockLearningTemplateCatalog.findWrongExplanation(question, normalizedUserAnswer);
        String correctExplanation = StringUtils.hasText(explanation)
                ? explanation
                : "해설을 다시 읽고 정답 근거를 확인해 보세요.";

        String feedback = StringUtils.hasText(wrongExplanation)
                ? "오답입니다. " + wrongExplanation + " 정답 근거: " + correctExplanation
                : "오답입니다. 정답 근거: " + correctExplanation;

        return new GradeResultDto(false, feedback);
    }

    private String buildWeakPointsMessage(int answeredCount, int averageLength, int structureHits, int metricHits) {
        List<String> weakPoints = new ArrayList<>();
        if (averageLength < 60) {
            weakPoints.add("답변 길이가 짧아 맥락과 근거가 충분히 드러나지 않았습니다.");
        }
        if (structureHits < answeredCount * 2) {
            weakPoints.add("상황, 행동, 결과가 분리되지 않아 논리 구조가 약하게 보입니다.");
        }
        if (metricHits == 0) {
            weakPoints.add("성과 수치나 결과 지표가 부족해 구체성이 낮아 보입니다.");
        }
        if (weakPoints.isEmpty()) {
            weakPoints.add("전반적으로 안정적인 답변이지만, 의사결정 이유를 더 설명하면 더 좋아집니다.");
        }
        return String.join(" ", weakPoints);
    }

    private String buildImprovementsMessage(int answeredCount, int structureHits, int metricHits, int keywordHits, int averageLength) {
        List<String> improvements = new ArrayList<>();
        if (averageLength < 60) {
            improvements.add("각 답변을 두세 문장 이상으로 작성해 문제 상황과 해결 맥락을 먼저 설명해 보세요.");
        }
        if (structureHits < answeredCount * 2) {
            improvements.add("STAR 방식처럼 상황, 행동, 결과를 순서대로 말해 보세요.");
        }
        if (metricHits == 0) {
            improvements.add("성능 수치, 일정, 사용자 수, 개선율 같은 정량 정보를 한 가지 이상 넣어 보세요.");
        }
        if (keywordHits < answeredCount * 2) {
            improvements.add("본인이 직접 사용한 기술과 해결 방법을 더 분명하게 설명해 보세요.");
        }
        if (improvements.isEmpty()) {
            improvements.add("현재 답변 흐름은 좋습니다. 다음에는 트레이드오프와 대안 비교까지 덧붙여 보세요.");
        }
        return String.join(" ", improvements);
    }

    private String buildRecommendedAnswerMessage(int averageLength, int metricHits) {
        if (averageLength < 60) {
            return "질문 의도를 먼저 짚고, 맡은 역할과 해결 과정을 설명한 뒤 결과와 배운 점을 마무리로 정리해 보세요.";
        }
        if (metricHits == 0) {
            return "답변 마지막에 성과 수치나 개선 결과를 한 문장으로 덧붙이면 더 설득력 있는 답변이 됩니다.";
        }
        return "상황, 행동, 결과를 유지하면서 기술 선택 이유와 대안을 함께 설명하면 더 완성도 높은 답변이 됩니다.";
    }

    private int countMatches(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }

        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private InterviewMode extractMode(InterviewQuestionGenerationContext context) {
        if (context != null && StringUtils.hasText(context.interviewMode())) {
            return InterviewMode.valueOf(context.interviewMode());
        }
        return InterviewMode.COMPREHENSIVE;
    }

    private InterviewQuestionCategory extractCategory(InterviewQuestionGenerationContext context) {
        if (context != null && "FRONTEND".equalsIgnoreCase(context.positionCategory())) {
            return InterviewQuestionCategory.FRONTEND;
        }
        return InterviewQuestionCategory.BACKEND;
    }

    private InterviewQuestionDifficulty extractDifficulty(InterviewQuestionGenerationContext context) {
        if (context != null && StringUtils.hasText(context.questionDifficulty())) {
            return InterviewQuestionDifficulty.valueOf(context.questionDifficulty());
        }
        return InterviewQuestionDifficulty.EASY;
    }
}
