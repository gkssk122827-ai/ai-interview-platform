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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
        List<String> materialHighlights = extractMaterialHighlights(context);

        if (!materialHighlights.isEmpty()) {
            String highlight = materialHighlights.get(Math.floorMod(Math.max(0, context.questionIndex() - 1), materialHighlights.size()));
            return buildDocumentAnchoredQuestion(highlight, mode, difficulty, context.questionIndex());
        }

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
        if (templatePool.isEmpty()) {
            return List.of();
        }

        List<MockLearningTemplateCatalog.Template> shuffledTemplates = new ArrayList<>(templatePool);
        Collections.shuffle(shuffledTemplates, ThreadLocalRandom.current());

        List<ProblemDto> problems = new ArrayList<>();
        for (int index = 1; index <= problemCount; index++) {
            MockLearningTemplateCatalog.Template template = shuffledTemplates.get((index - 1) % shuffledTemplates.size());
            problems.add(new ProblemDto(
                    "MULTIPLE",
                    template.question(),
                    template.choices(),
                    template.answer(),
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

    private List<String> extractMaterialHighlights(InterviewQuestionGenerationContext context) {
        if (context == null || context.materialHighlights() == null || context.materialHighlights().isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        for (String highlight : context.materialHighlights()) {
            String normalized = normalizeHighlight(highlight);
            if (StringUtils.hasText(normalized)) {
                highlights.add(normalized);
            }
        }
        return List.copyOf(highlights);
    }

    private String normalizeHighlight(String highlight) {
        if (!StringUtils.hasText(highlight)) {
            return null;
        }

        String normalized = highlight.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 117).trim() + "...";
        }
        return normalized.length() < 12 ? null : normalized;
    }

    private String buildDocumentAnchoredQuestion(
            String highlight,
            InterviewMode mode,
            InterviewQuestionDifficulty difficulty
    ) {
        String quotedHighlight = "'" + highlight + "'";
        return switch (mode) {
            case BEHAVIORAL -> switch (difficulty) {
                case EASY -> quotedHighlight + " 경험을 진행할 때 맡았던 역할과 협업 방식에 대해 설명해 주세요.";
                case MEDIUM -> quotedHighlight + " 경험에서 예상과 다른 문제가 생겼을 때 어떻게 조율하고 해결했는지 구체적으로 말씀해 주세요.";
                case HARD -> quotedHighlight + " 경험에서 본인이 주도적으로 내린 판단이 무엇이었고, 그 결정의 근거와 결과를 설명해 주세요.";
            };
            case TECHNICAL -> switch (difficulty) {
                case EASY -> quotedHighlight + "와 관련해 실제로 사용한 기술과 구현 방식을 설명해 주세요.";
                case MEDIUM -> quotedHighlight + "를 수행하면서 마주친 기술적 문제와 그것을 해결한 과정을 구체적으로 설명해 주세요.";
                case HARD -> quotedHighlight + "에서 기술 선택이나 아키텍처를 결정할 때 고려한 트레이드오프를 설명해 주세요.";
            };
            case RESUME_BASED -> switch (difficulty) {
                case EASY -> "지원 자료에 적힌 " + quotedHighlight + " 내용을 바탕으로 직접 기여한 부분을 설명해 주세요.";
                case MEDIUM -> "지원 자료에 적힌 " + quotedHighlight + " 내용이 실제 프로젝트에서 어떤 성과로 이어졌는지 구체적으로 말씀해 주세요.";
                case HARD -> "지원 자료에 적힌 " + quotedHighlight + " 주장이나 성과를 면접에서 검증한다면, 어떤 근거와 지표로 설명하시겠습니까?";
            };
            case COMPREHENSIVE -> switch (difficulty) {
                case EASY -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 소개하고, 그 경험이 지원 직무와 어떻게 연결되는지 설명해 주세요.";
                case MEDIUM -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 문제 해결 과정과 협업 방식을 함께 설명해 주세요.";
                case HARD -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 기준으로, 본인의 기술적 판단과 비즈니스 판단을 어떻게 균형 있게 가져갔는지 설명해 주세요.";
            };
        };
    }

    private String buildDocumentAnchoredQuestion(
            String highlight,
            InterviewMode mode,
            InterviewQuestionDifficulty difficulty,
            int questionSeed
    ) {
        String quotedHighlight = "'" + highlight + "'";
        String focus = resolveQuestionFocus(mode, difficulty, questionSeed);
        return switch (mode) {
            case BEHAVIORAL -> switch (difficulty) {
                case EASY -> switch (focus) {
                    case "collaboration" -> quotedHighlight + " 경험에서 누구와 어떻게 협업했는지 설명해 주세요.";
                    case "motivation" -> quotedHighlight + " 경험을 맡게 된 배경과 동기를 설명해 주세요.";
                    default -> quotedHighlight + " 경험에서 맡았던 역할과 책임 범위를 설명해 주세요.";
                };
                case MEDIUM -> switch (focus) {
                    case "communication" -> quotedHighlight + " 경험에서 이해관계자와 조율이 필요했던 상황을 어떻게 풀었는지 설명해 주세요.";
                    case "ownership" -> quotedHighlight + " 경험에서 본인이 먼저 문제를 발견하고 주도적으로 움직인 장면을 설명해 주세요.";
                    default -> quotedHighlight + " 경험에서 예상과 다른 문제가 생겼을 때 어떻게 해결했는지 설명해 주세요.";
                };
                case HARD -> switch (focus) {
                    case "decision" -> quotedHighlight + " 경험에서 쉽지 않은 결정을 내렸던 순간과 그 근거를 설명해 주세요.";
                    case "tradeoff" -> quotedHighlight + " 경험에서 품질, 일정, 범위 사이의 트레이드오프를 어떻게 판단했는지 설명해 주세요.";
                    default -> quotedHighlight + " 경험에서 리스크를 감수하고 책임 있게 대응했던 사례를 설명해 주세요.";
                };
            };
            case TECHNICAL -> switch (difficulty) {
                case EASY -> switch (focus) {
                    case "rendering" -> quotedHighlight + "와 관련한 화면 동작이나 렌더링 흐름을 어떻게 구현했는지 설명해 주세요.";
                    case "integration" -> quotedHighlight + "를 구현할 때 API 또는 상태와 어떻게 연결했는지 설명해 주세요.";
                    default -> quotedHighlight + "와 관련해 실제 사용한 기술 스택과 구현 방식을 설명해 주세요.";
                };
                case MEDIUM -> switch (focus) {
                    case "performance" -> quotedHighlight + "를 수행하면서 성능 병목을 어떻게 찾고 개선했는지 설명해 주세요.";
                    case "state" -> quotedHighlight + "를 구현할 때 상태 관리나 데이터 흐름을 어떻게 설계했는지 설명해 주세요.";
                    default -> quotedHighlight + "를 진행하면서 겪은 기술적 문제와 해결 과정을 설명해 주세요.";
                };
                case HARD -> switch (focus) {
                    case "tradeoff" -> quotedHighlight + "에서 기술 선택의 트레이드오프를 어떻게 판단했는지 설명해 주세요.";
                    case "quality" -> quotedHighlight + "를 장기적으로 유지보수하기 위해 테스트나 품질 전략을 어떻게 가져갔는지 설명해 주세요.";
                    default -> quotedHighlight + "에서 아키텍처나 구조를 결정할 때 고려한 핵심 기준을 설명해 주세요.";
                };
            };
            case RESUME_BASED -> switch (difficulty) {
                case EASY -> switch (focus) {
                    case "contribution" -> "지원 자료에 적힌 " + quotedHighlight + " 내용에서 본인이 직접 기여한 부분을 설명해 주세요.";
                    case "context" -> "지원 자료에 적힌 " + quotedHighlight + " 내용이 어떤 맥락에서 나온 것인지 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 내용을 바탕으로 핵심 경험을 소개해 주세요.";
                };
                case MEDIUM -> switch (focus) {
                    case "outcome" -> "지원 자료에 적힌 " + quotedHighlight + " 내용이 실제 결과로 이어진 과정을 설명해 주세요.";
                    case "problem" -> "지원 자료에 적힌 " + quotedHighlight + " 내용에서 어떤 문제를 정의하고 해결했는지 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 내용을 근거와 함께 구체적으로 설명해 주세요.";
                };
                case HARD -> switch (focus) {
                    case "metrics" -> "지원 자료에 적힌 " + quotedHighlight + " 성과를 어떤 지표와 데이터로 검증할 수 있는지 설명해 주세요.";
                    case "tradeoff" -> "지원 자료에 적힌 " + quotedHighlight + " 결정에서 아쉬웠던 점이나 대안까지 함께 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 주장에 대해 어떻게 검증받을 수 있는지 설명해 주세요.";
                };
            };
            case COMPREHENSIVE -> switch (difficulty) {
                case EASY -> switch (focus) {
                    case "strength" -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 소개하고, 이 경험이 본인의 강점과 어떻게 연결되는지 설명해 주세요.";
                    case "fit" -> "지원 자료에 적힌 " + quotedHighlight + " 경험이 지원 직무와 어떻게 맞닿아 있는지 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 간단히 소개해 주세요.";
                };
                case MEDIUM -> switch (focus) {
                    case "collaboration" -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 바탕으로 문제 해결과 협업 과정을 함께 설명해 주세요.";
                    case "decision" -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 어떤 판단을 했고 왜 그렇게 결정했는지 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 핵심 문제와 해결 과정을 설명해 주세요.";
                };
                case HARD -> switch (focus) {
                    case "business" -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 기술 판단과 비즈니스 판단을 어떻게 균형 있게 가져갔는지 설명해 주세요.";
                    case "retrospective" -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 다시 한다면 무엇을 다르게 할지 설명해 주세요.";
                    default -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 가장 어려웠던 트레이드오프를 설명해 주세요.";
                };
            };
        };
    }

    private String resolveQuestionFocus(InterviewMode mode, InterviewQuestionDifficulty difficulty, int questionSeed) {
        List<String> focuses = switch (mode) {
            case BEHAVIORAL -> switch (difficulty) {
                case EASY -> List.of("role", "collaboration", "motivation");
                case MEDIUM -> List.of("problem", "communication", "ownership");
                case HARD -> List.of("decision", "tradeoff", "risk");
            };
            case TECHNICAL -> switch (difficulty) {
                case EASY -> List.of("implementation", "rendering", "integration");
                case MEDIUM -> List.of("problem", "performance", "state");
                case HARD -> List.of("architecture", "tradeoff", "quality");
            };
            case RESUME_BASED -> switch (difficulty) {
                case EASY -> List.of("contribution", "context", "summary");
                case MEDIUM -> List.of("outcome", "problem", "evidence");
                case HARD -> List.of("metrics", "tradeoff", "validation");
            };
            case COMPREHENSIVE -> switch (difficulty) {
                case EASY -> List.of("strength", "fit", "summary");
                case MEDIUM -> List.of("collaboration", "decision", "problem");
                case HARD -> List.of("business", "retrospective", "tradeoff");
            };
        };
        return focuses.get(Math.floorMod(Math.max(1, questionSeed) - 1, focuses.size()));
    }
}
