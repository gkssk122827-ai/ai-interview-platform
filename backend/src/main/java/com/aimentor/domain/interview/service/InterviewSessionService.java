package com.aimentor.domain.interview.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.interview.dto.request.SaveInterviewAnswerRequest;
import com.aimentor.domain.interview.dto.request.StartInterviewSessionRequest;
import com.aimentor.domain.interview.dto.response.InterviewAnswerResponse;
import com.aimentor.domain.interview.dto.response.InterviewFeedbackResponse;
import com.aimentor.domain.interview.dto.response.InterviewLearningRecommendationResponse;
import com.aimentor.domain.interview.dto.response.InterviewQuestionResponse;
import com.aimentor.domain.interview.dto.response.InterviewResultReportResponse;
import com.aimentor.domain.interview.dto.response.InterviewSessionResponse;
import com.aimentor.domain.interview.entity.InterviewMode;
import com.aimentor.domain.interview.service.InterviewQuestionCatalog.InterviewQuestionCategory;
import com.aimentor.domain.interview.service.InterviewQuestionCatalog.InterviewQuestionDifficulty;
import com.aimentor.domain.interview.entity.InterviewAnswer;
import com.aimentor.domain.interview.entity.InterviewFeedback;
import com.aimentor.domain.interview.entity.InterviewQuestion;
import com.aimentor.domain.interview.entity.InterviewSession;
import com.aimentor.domain.interview.entity.InterviewSessionStatus;
import com.aimentor.domain.interview.repository.InterviewAnswerRepository;
import com.aimentor.domain.interview.repository.InterviewFeedbackRepository;
import com.aimentor.domain.interview.repository.InterviewQuestionRepository;
import com.aimentor.domain.interview.repository.InterviewSessionRepository;
import com.aimentor.domain.profile.entity.ApplicationDocument;
import com.aimentor.domain.profile.entity.CoverLetter;
import com.aimentor.domain.profile.entity.JobPosting;
import com.aimentor.domain.profile.entity.Resume;
import com.aimentor.domain.profile.repository.ApplicationDocumentRepository;
import com.aimentor.domain.profile.repository.CoverLetterRepository;
import com.aimentor.domain.profile.repository.JobPostingRepository;
import com.aimentor.domain.profile.repository.ResumeRepository;
import com.aimentor.domain.profile.service.ApplicationDocumentService;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import com.aimentor.external.ai.AiService;
import com.aimentor.external.ai.ConversationTurnDto;
import com.aimentor.external.ai.dto.FeedbackDto;
import com.aimentor.external.ai.dto.InterviewQuestionGenerationContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Handles interview session lifecycle operations and builds summary data for result reporting.
 */
@Service
@Transactional(readOnly = true)
public class InterviewSessionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSessionService.class);
    private static final int DEFAULT_QUESTION_COUNT = 3;
    private static final int MAX_CONTEXT_LENGTH = 3000;
    private static final int MAX_AI_GENERATION_RETRIES = 5;
    private static final double QUESTION_SIMILARITY_THRESHOLD = 0.75;
    private static final Set<String> QUESTION_STOP_WORDS = Set.of(
            "무엇", "설명", "주세요", "말해", "있다면", "있나요", "어떻게", "경험", "기술", "프로젝트", "기반", "지원서", "이력서", "직무"
    );

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final UserRepository userRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ResumeRepository resumeRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationDocumentService applicationDocumentService;
    private final AiService aiService;

    public InterviewSessionService(
            InterviewSessionRepository interviewSessionRepository,
            InterviewQuestionRepository interviewQuestionRepository,
            InterviewAnswerRepository interviewAnswerRepository,
            InterviewFeedbackRepository interviewFeedbackRepository,
            UserRepository userRepository,
            ApplicationDocumentRepository applicationDocumentRepository,
            ResumeRepository resumeRepository,
            CoverLetterRepository coverLetterRepository,
            JobPostingRepository jobPostingRepository,
            ApplicationDocumentService applicationDocumentService,
            AiService aiService
    ) {
        this.interviewSessionRepository = interviewSessionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewAnswerRepository = interviewAnswerRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
        this.userRepository = userRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.resumeRepository = resumeRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationDocumentService = applicationDocumentService;
        this.aiService = aiService;
    }

    @Transactional
    public InterviewSessionResponse startSession(Long userId, StartInterviewSessionRequest request) {
        User user = getUser(userId);
        ApplicationDocument applicationDocument = getOwnedApplicationDocument(userId, request.applicationDocumentId());
        Resume resume = applicationDocument == null ? getOwnedResume(userId, request.resumeId()) : null;
        CoverLetter coverLetter = applicationDocument == null ? getOwnedCoverLetter(userId, request.coverLetterId()) : null;
        JobPosting jobPosting = getJobPosting(request.jobPostingId());

        InterviewSession interviewSession = InterviewSession.builder()
                .user(user)
                .title(request.title())
                .positionTitle(request.positionTitle())
                .mode(resolveInterviewMode(request.mode()))
                .applicationDocumentId(applicationDocument == null ? null : applicationDocument.getId())
                .resumeId(request.resumeId())
                .coverLetterId(request.coverLetterId())
                .jobPostingId(request.jobPostingId())
                .applicationDocumentSnapshot(buildApplicationDocumentSnapshot(applicationDocument))
                .resumeSnapshot(applicationDocument == null ? buildResumeSnapshot(resume) : buildResumeSnapshotFromApplicationDocument(applicationDocument))
                .coverLetterSnapshot(applicationDocument == null ? buildCoverLetterSnapshot(coverLetter) : buildCoverLetterSnapshotFromApplicationDocument(applicationDocument))
                .jobPostingSnapshot(buildJobPostingSnapshot(jobPosting))
                .status(InterviewSessionStatus.ONGOING)
                .startedAt(LocalDateTime.now())
                .build();

        InterviewSession savedSession = interviewSessionRepository.save(interviewSession);
        QuestionGenerationResult questionGenerationResult = buildInterviewQuestions(savedSession, request.questionCount());
        for (InterviewQuestion question : questionGenerationResult.questions()) {
            InterviewQuestion savedQuestion = interviewQuestionRepository.save(question);
            savedSession.addQuestion(savedQuestion);
        }

        replaceSessionFeedback(savedSession, buildPendingFeedback());
        return toSessionResponse(
                savedSession,
                questionGenerationResult.source(),
                questionGenerationResult.fallbackUsed(),
                questionGenerationResult.message()
        );
    }

    public List<InterviewSessionResponse> listSessions(Long userId) {
        return interviewSessionRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public InterviewSessionResponse getSessionDetail(Long userId, Long sessionId) {
        return toSessionResponse(getOwnedSession(userId, sessionId));
    }

    @Transactional
    public InterviewAnswerResponse saveAnswer(Long userId, Long sessionId, SaveInterviewAnswerRequest request) {
        InterviewSession interviewSession = getOwnedSession(userId, sessionId);

        if (interviewSession.getStatus() == InterviewSessionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SESSION_ALREADY_COMPLETED", "Interview session is already completed.");
        }

        InterviewQuestion interviewQuestion = interviewQuestionRepository
                .findByIdAndInterviewSessionIdAndInterviewSessionUserId(request.questionId(), sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Interview question not found."));

        String audioUrl = request.audioUrl() == null || request.audioUrl().isBlank()
                ? "https://mock-s3.local/interviews/" + sessionId + "/answers/" + request.questionId() + ".wav"
                : request.audioUrl();

        InterviewAnswer answer = interviewQuestion.getAnswer();
        if (answer == null) {
            InterviewAnswer newAnswer = InterviewAnswer.builder()
                    .interviewQuestion(interviewQuestion)
                    .answerText(request.answerText())
                    .audioUrl(audioUrl)
                    .build();
            InterviewAnswer savedAnswer = interviewAnswerRepository.save(newAnswer);
            interviewQuestion.assignAnswer(savedAnswer);
            replaceSessionFeedback(interviewSession, buildSessionFeedback(interviewSession));
            return toAnswerResponse(savedAnswer);
        }

        answer.update(request.answerText(), audioUrl);
        replaceSessionFeedback(interviewSession, buildSessionFeedback(interviewSession));
        return toAnswerResponse(answer);
    }

    @Transactional
    public InterviewSessionResponse endSession(Long userId, Long sessionId) {
        InterviewSession interviewSession = getOwnedSession(userId, sessionId);

        if (interviewSession.getStatus() == InterviewSessionStatus.COMPLETED) {
            return toSessionResponse(interviewSession);
        }

        replaceSessionFeedback(interviewSession, buildSessionFeedback(interviewSession));
        interviewSession.end();
        return toSessionResponse(interviewSession);
    }

    public InterviewFeedbackResponse getFeedback(Long userId, Long sessionId) {
        InterviewSession interviewSession = getOwnedSession(userId, sessionId);
        return toFeedbackResponse(interviewSession.getFeedback());
    }

    public InterviewResultReportResponse getResultReport(Long userId, Long sessionId) {
        InterviewSession interviewSession = getOwnedSession(userId, sessionId);
        int totalQuestions = interviewSession.getTotalQuestionCount();
        int answeredQuestions = interviewSession.getAnsweredQuestionCount();
        int unansweredQuestions = totalQuestions - answeredQuestions;
        int completionRate = calculateCompletionRate(totalQuestions, answeredQuestions);
        InterviewFeedbackResponse feedback = toFeedbackResponse(interviewSession.getFeedback());

        return new InterviewResultReportResponse(
                interviewSession.getId(),
                interviewSession.getUser().getId(),
                interviewSession.getApplicationDocumentId(),
                interviewSession.getResumeId(),
                interviewSession.getCoverLetterId(),
                interviewSession.getJobPostingId(),
                interviewSession.getTitle(),
                interviewSession.getPositionTitle(),
                interviewSession.getMode(),
                interviewSession.getStatus(),
                interviewSession.getStartedAt(),
                interviewSession.getEndedAt(),
                totalQuestions,
                answeredQuestions,
                unansweredQuestions,
                completionRate,
                interviewSession.getStatus() == InterviewSessionStatus.COMPLETED,
                calculateDurationMinutes(interviewSession),
                buildResultSummary(feedback, totalQuestions, answeredQuestions),
                buildHighlights(interviewSession, feedback, totalQuestions, answeredQuestions),
                buildLearningRecommendations(feedback, unansweredQuestions),
                interviewSession.getQuestions().stream()
                        .sorted(Comparator.comparing(InterviewQuestion::getSequenceNumber))
                        .map(this::toQuestionResponse)
                        .toList(),
                feedback
        );
    }

    private QuestionGenerationResult buildInterviewQuestions(InterviewSession session, Integer requestedQuestionCount) {
        int questionCount = requestedQuestionCount == null ? DEFAULT_QUESTION_COUNT : requestedQuestionCount;
        try {
            List<InterviewQuestion> aiQuestions = buildAiQuestions(session, questionCount);
            return new QuestionGenerationResult(
                    aiQuestions,
                    "AI",
                    false,
                    "AI 기반으로 면접 질문을 생성했습니다."
            );
        } catch (RuntimeException ex) {
            log.warn("[Interview] Falling back to mock question generation. sessionId={}", session.getId(), ex);
            return new QuestionGenerationResult(
                    buildMockQuestions(session, questionCount),
                    "MOCK",
                    true,
                    "AI 질문 생성에 일시적인 문제가 있어 기본 질문으로 면접을 시작했습니다."
            );
        }
    }
    private List<InterviewQuestion> buildAiQuestions(InterviewSession session, int questionCount) {
        List<InterviewQuestion> questions = new ArrayList<>();
        List<String> generatedQuestions = new ArrayList<>();
        InterviewMode mode = resolveInterviewMode(session.getMode());
        InterviewQuestionCategory category = InterviewQuestionCatalog.resolveCategoryFromContext(
                session.getPositionTitle(),
                session.getJobPostingSnapshot(),
                session.getApplicationDocumentSnapshot(),
                session.getResumeSnapshot(),
                session.getCoverLetterSnapshot()
        );

        for (int index = 1; index <= questionCount; index++) {
            InterviewQuestionDifficulty difficulty = InterviewQuestionCatalog.resolveDifficulty(index, questionCount);
            String normalizedQuestionText;
            try {
                normalizedQuestionText = generateAiQuestionWithRetry(
                        session,
                        generatedQuestions,
                        mode,
                        category,
                        difficulty,
                        index,
                        questionCount
                );
            } catch (RuntimeException ex) {
                log.warn(
                        "[Interview] Falling back to slot-level question template. sessionId={}, questionIndex={}",
                        session.getId(),
                        index,
                        ex
                );
                normalizedQuestionText = buildFallbackQuestionText(
                        session,
                        generatedQuestions,
                        index,
                        mode,
                        category,
                        difficulty
                );
            }

            generatedQuestions.add(normalizedQuestionText);
            questions.add(InterviewQuestion.builder()
                    .interviewSession(session)
                    .sequenceNumber(index)
                    .questionText(normalizedQuestionText)
                    .build());
        }

        return questions;
    }

    private List<InterviewQuestion> buildMockQuestions(InterviewSession session, int questionCount) {
        List<InterviewQuestion> questions = new ArrayList<>();
        List<String> generatedQuestions = new ArrayList<>();
        InterviewMode mode = resolveInterviewMode(session.getMode());
        InterviewQuestionCategory category = InterviewQuestionCatalog.resolveCategoryFromContext(
                session.getPositionTitle(),
                session.getJobPostingSnapshot(),
                session.getApplicationDocumentSnapshot(),
                session.getResumeSnapshot(),
                session.getCoverLetterSnapshot()
        );
        for (int index = 1; index <= questionCount; index++) {
            InterviewQuestionDifficulty difficulty = InterviewQuestionCatalog.resolveDifficulty(index, questionCount);
            String questionText = buildFallbackQuestionText(session, generatedQuestions, index, mode, category, difficulty);
            generatedQuestions.add(questionText);
            questions.add(InterviewQuestion.builder()
                    .interviewSession(session)
                    .sequenceNumber(index)
                    .questionText(questionText)
                    .build());
        }
        return questions;
    }

    private String buildFallbackQuestionText(
            InterviewSession session,
            List<String> generatedQuestions,
            int index,
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty
    ) {
        List<String> materialHighlights = buildMaterialHighlights(session);
        String contextualQuestion = buildContextualFallbackQuestion(materialHighlights, generatedQuestions, index, mode, difficulty);
        if (StringUtils.hasText(contextualQuestion)) {
            return contextualQuestion;
        }

        List<String> questions = InterviewQuestionCatalog.findQuestions(mode, category, difficulty);
        String candidate = selectQuestionCandidate(session, questions, generatedQuestions, index, difficulty);
        if (StringUtils.hasText(session.getPositionTitle())
                && !candidate.contains(session.getPositionTitle())
                && index == 1) {
            return session.getPositionTitle() + " 직무를 기준으로 답변해 주세요. " + candidate;
        }
        return candidate;
    }
    private FeedbackDto buildPendingFeedback() {
        return new FeedbackDto(
                0,
                0,
                0,
                0,
                "아직 저장된 답변이 없어 결과를 분석할 수 없습니다.",
                "각 답변을 두세 문장 이상으로 작성하고, 본인의 역할과 선택 이유를 함께 설명해 보세요.",
                "상황, 행동, 결과를 순서대로 정리하고 성과나 수치 근거를 덧붙이면 더 좋은 답변이 됩니다."
        );
    }
    private FeedbackDto buildSessionFeedback(InterviewSession session) {
        if (session.getAnsweredQuestionCount() == 0) {
            return buildPendingFeedback();
        }

        List<ConversationTurnDto> history = buildConversationHistory(session);
        try {
            FeedbackDto feedback = aiService.generateFeedback(history);
            if (feedback != null && feedback.overallScore() != null) {
                return feedback;
            }
        } catch (RuntimeException ex) {
            log.warn("[Interview] Falling back to local feedback generation. sessionId={}", session.getId(), ex);
        }
        return buildFallbackFeedback(session);
    }

    private FeedbackDto buildFallbackFeedback(InterviewSession session) {
        List<ConversationTurnDto> history = buildConversationHistory(session);
        AnswerAnalysis analysis = analyzeAnswers(history);

        int relevanceScore = clampScore(48 + analysis.answeredCount() * 8 + analysis.keywordHits() * 4 + analysis.averageLength() / 18);
        int logicScore = clampScore(45 + analysis.answeredCount() * 7 + analysis.structureHits() * 6 + analysis.averageLength() / 22);
        int specificityScore = clampScore(40 + analysis.answeredCount() * 7 + analysis.metricHits() * 8 + analysis.averageLength() / 16);
        int overallScore = Math.round((relevanceScore + logicScore + specificityScore) / 3.0f);

        String weakPoints = buildWeakPointsMessage(analysis, session.getAnsweredQuestionCount(), session.getTotalQuestionCount());
        String improvements = buildImprovementsMessage(analysis);
        String recommendedAnswer = buildRecommendedAnswerMessage(analysis);

        return new FeedbackDto(
                logicScore,
                relevanceScore,
                specificityScore,
                overallScore,
                weakPoints,
                improvements,
                recommendedAnswer
        );
    }

    private AnswerAnalysis analyzeAnswers(List<ConversationTurnDto> history) {
        int answeredCount = history == null ? 0 : history.size();
        if (answeredCount == 0) {
            return new AnswerAnalysis(0, 0, 0, 0, 0);
        }
        int totalLength = 0;
        int structureHits = 0;
        int metricHits = 0;
        int keywordHits = 0;
        for (ConversationTurnDto turn : history) {
            String answer = turn.answer() == null ? "" : turn.answer().trim();
            totalLength += answer.length();
            structureHits += countMatches(answer, "상황", "문제", "목표", "과정", "행동", "결과", "배운", "이유");
            metricHits += countMatches(answer, "%", "건", "명", "개월", "주", "배", "ms", "초", "성능", "지표");
            keywordHits += countMatches(answer, "사용", "구현", "개선", "설계", "작업", "테스트", "배포", "최적화", "해결");
        }
        int averageLength = totalLength / answeredCount;
        return new AnswerAnalysis(answeredCount, averageLength, structureHits, metricHits, keywordHits);
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

    private String buildWeakPointsMessage(AnswerAnalysis analysis, int answeredCount, int totalQuestionCount) {
        List<String> weakPoints = new ArrayList<>();
        if (answeredCount < totalQuestionCount) {
            weakPoints.add("아직 답변하지 않은 질문이 남아 있어 전체 평가가 제한적입니다.");
        }
        if (analysis.averageLength() < 60) {
            weakPoints.add("답변 길이가 짧아 핵심 근거와 맥락이 충분히 드러나지 않았습니다.");
        }
        if (analysis.metricHits() == 0) {
            weakPoints.add("성과 수치나 결과 지표가 부족해 구체성이 약하게 보입니다.");
        }
        if (analysis.structureHits() < answeredCount * 2) {
            weakPoints.add("상황, 행동, 결과가 분리되지 않아 답변 구조가 다소 흐릿합니다.");
        }
        if (weakPoints.isEmpty()) {
            weakPoints.add("전반적으로 안정적이지만, 조금 더 구체적인 사례를 더하면 설득력이 높아집니다.");
        }
        return String.join(" ", weakPoints);
    }
    private String buildImprovementsMessage(AnswerAnalysis analysis) {
        List<String> improvements = new ArrayList<>();
        if (analysis.averageLength() < 60) {
            improvements.add("각 답변을 두세 문장 이상으로 확장해 맥락을 먼저 설명해 보세요.");
        }
        if (analysis.structureHits() < analysis.answeredCount() * 2) {
            improvements.add("STAR 방식처럼 상황, 행동, 결과를 순서대로 나누어 답변해 보세요.");
        }
        if (analysis.metricHits() == 0) {
            improvements.add("성능 개선 수치, 일정, 사용자 수 같은 정량 정보를 한 가지 이상 포함해 보세요.");
        }
        if (analysis.keywordHits() < analysis.answeredCount() * 2) {
            improvements.add("본인이 직접 사용한 기술과 해결 방법을 더 분명하게 설명해 보세요.");
        }
        if (improvements.isEmpty()) {
            improvements.add("현재 답변 구조는 좋습니다. 다음 연습에서는 의사결정 이유와 트레이드오프까지 덧붙여 보세요.");
        }
        return String.join(" ", improvements);
    }
    private String buildRecommendedAnswerMessage(AnswerAnalysis analysis) {
        if (analysis.averageLength() < 60) {
            return "질문 의도를 먼저 짚고, 본인이 맡은 역할과 해결 과정을 설명한 뒤 마지막에 결과와 배운 점을 정리해 보세요.";
        }
        if (analysis.metricHits() == 0) {
            return "답변 마지막에 성과 수치나 개선 결과를 한 문장으로 덧붙이면 훨씬 설득력 있는 답변이 됩니다.";
        }
        return "상황, 행동, 결과를 유지하면서 기술 선택 이유와 다른 대안까지 설명하면 더 완성도 높은 답변이 됩니다.";
    }
    private void replaceSessionFeedback(InterviewSession interviewSession, FeedbackDto content) {
        InterviewFeedback currentFeedback = interviewSession.getFeedback();
        if (currentFeedback != null) {
            currentFeedback.update(
                    content.relevanceScore(),
                    content.logicScore(),
                    content.specificityScore(),
                    content.overallScore(),
                    content.weakPoints(),
                    content.improvements(),
                    content.recommendedAnswer()
            );
            return;
        }

        InterviewFeedback feedback = InterviewFeedback.builder()
                .interviewSession(interviewSession)
                .relevanceScore(content.relevanceScore())
                .logicScore(content.logicScore())
                .specificityScore(content.specificityScore())
                .overallScore(content.overallScore())
                .weakPoints(content.weakPoints())
                .improvements(content.improvements())
                .recommendedAnswer(content.recommendedAnswer())
                .build();
        interviewSession.assignFeedback(interviewFeedbackRepository.save(feedback));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private ApplicationDocument getOwnedApplicationDocument(Long userId, Long applicationDocumentId) {
        if (applicationDocumentId == null) {
            return null;
        }
        return applicationDocumentRepository.findByIdAndUserId(applicationDocumentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_DOCUMENT_NOT_FOUND", "Profile document not found."));
    }

    private Resume getOwnedResume(Long userId, Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND", "Resume not found."));
    }

    private CoverLetter getOwnedCoverLetter(Long userId, Long coverLetterId) {
        if (coverLetterId == null) {
            return null;
        }
        return coverLetterRepository.findByIdAndUserId(coverLetterId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COVER_LETTER_NOT_FOUND", "Cover letter not found."));
    }

    private JobPosting getJobPosting(Long jobPostingId) {
        if (jobPostingId == null) {
            return null;
        }
        return jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_POSTING_NOT_FOUND", "Job posting not found."));
    }

    private InterviewSession getOwnedSession(Long userId, Long sessionId) {
        return interviewSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_SESSION_NOT_FOUND", "Interview session not found."));
    }

    private List<ConversationTurnDto> buildConversationHistory(InterviewSession session) {
        return session.getQuestions().stream()
                .sorted(Comparator.comparing(InterviewQuestion::getSequenceNumber))
                .filter(question -> question.getAnswer() != null
                        && question.getAnswer().getAnswerText() != null
                        && !question.getAnswer().getAnswerText().isBlank())
                .map(question -> new ConversationTurnDto(question.getQuestionText(), question.getAnswer().getAnswerText()))
                .toList();
    }

    private String buildApplicationDocumentSnapshot(ApplicationDocument applicationDocument) {
        if (applicationDocument == null) {
            return null;
        }

        String extractedFileText = applicationDocumentService.extractStoredFileText(applicationDocument);
        StringBuilder builder = new StringBuilder();
        builder.append("Document title: ").append(applicationDocument.getTitle());
        if (StringUtils.hasText(applicationDocument.getResumeText())) {
            builder.append("\nResume text: ").append(applicationDocument.getResumeText());
        }
        if (StringUtils.hasText(applicationDocument.getCoverLetterText())) {
            builder.append("\nCover letter text: ").append(applicationDocument.getCoverLetterText());
        }
        if (StringUtils.hasText(applicationDocument.getOriginalFileName())) {
            builder.append("\nOriginal file name: ").append(applicationDocument.getOriginalFileName());
        }
        if (StringUtils.hasText(applicationDocument.getFileUrl())) {
            builder.append("\nFile URL: ").append(applicationDocument.getFileUrl());
        }
        if (StringUtils.hasText(extractedFileText)) {
            builder.append("\nExtracted file text: ").append(extractedFileText);
        }
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildResumeSnapshot(Resume resume) {
        if (resume == null) {
            return null;
        }
        return limitText("Resume title: " + resume.getTitle() + "\nResume content: " + resume.getContent(), MAX_CONTEXT_LENGTH);
    }

    private String buildResumeSnapshotFromApplicationDocument(ApplicationDocument applicationDocument) {
        if (applicationDocument == null) {
            return null;
        }

        String resumeText = StringUtils.hasText(applicationDocument.getResumeText())
                ? applicationDocument.getResumeText()
                : applicationDocumentService.extractStoredFileText(applicationDocument);
        if (!StringUtils.hasText(resumeText)) {
            return null;
        }
        return limitText(
                "Resume title: " + applicationDocument.getTitle() + "\nResume content: " + resumeText,
                MAX_CONTEXT_LENGTH
        );
    }

    private String buildCoverLetterSnapshot(CoverLetter coverLetter) {
        if (coverLetter == null) {
            return null;
        }
        return limitText(
                "Cover letter title: " + coverLetter.getTitle()
                        + "\nCompany: " + coverLetter.getCompanyName()
                        + "\nContent: " + coverLetter.getContent(),
                MAX_CONTEXT_LENGTH
        );
    }

    private String buildCoverLetterSnapshotFromApplicationDocument(ApplicationDocument applicationDocument) {
        if (applicationDocument == null) {
            return null;
        }

        String coverLetterText = StringUtils.hasText(applicationDocument.getCoverLetterText())
                ? applicationDocument.getCoverLetterText()
                : applicationDocumentService.extractStoredFileText(applicationDocument);
        if (!StringUtils.hasText(coverLetterText)) {
            return null;
        }
        return limitText(
                "Cover letter title: " + applicationDocument.getTitle()
                        + "\nContent: " + coverLetterText,
                MAX_CONTEXT_LENGTH
        );
    }

    private String buildJobPostingSnapshot(JobPosting jobPosting) {
        if (jobPosting == null) {
            return null;
        }
        return limitText(
                "Company: " + jobPosting.getCompanyName()
                        + "\nPosition: " + jobPosting.getPositionTitle()
                        + "\nDescription: " + jobPosting.getDescription(),
                MAX_CONTEXT_LENGTH
        );
    }

    private InterviewSessionResponse toSessionResponse(InterviewSession interviewSession) {
        return toSessionResponse(interviewSession, null, null, null);
    }

    private InterviewSessionResponse toSessionResponse(
            InterviewSession interviewSession,
            String questionGenerationSource,
            Boolean questionGenerationFallbackUsed,
            String questionGenerationMessage
    ) {
        int totalQuestions = interviewSession.getTotalQuestionCount();
        int answeredQuestions = interviewSession.getAnsweredQuestionCount();
        return new InterviewSessionResponse(
                interviewSession.getId(),
                interviewSession.getUser().getId(),
                interviewSession.getApplicationDocumentId(),
                interviewSession.getResumeId(),
                interviewSession.getCoverLetterId(),
                interviewSession.getJobPostingId(),
                interviewSession.getTitle(),
                interviewSession.getPositionTitle(),
                interviewSession.getMode(),
                interviewSession.getStatus(),
                interviewSession.getStartedAt(),
                interviewSession.getEndedAt(),
                totalQuestions,
                answeredQuestions,
                totalQuestions - answeredQuestions,
                calculateCompletionRate(totalQuestions, answeredQuestions),
                interviewSession.getQuestions().stream()
                        .sorted(Comparator.comparing(InterviewQuestion::getSequenceNumber))
                        .map(this::toQuestionResponse)
                        .toList(),
                toFeedbackResponse(interviewSession.getFeedback()),
                questionGenerationSource,
                questionGenerationFallbackUsed,
                questionGenerationMessage
        );
    }

    private String buildResumeGenerationInput(InterviewSession session) {
        StringBuilder builder = new StringBuilder();
        appendLabeledSection(builder, "Session title", session.getTitle());
        appendLabeledSection(builder, "Target position", session.getPositionTitle());
        appendLabeledSection(builder, "Interview mode", resolveInterviewMode(session.getMode()).name());
        appendLabeledSection(builder, "Application document snapshot", session.getApplicationDocumentSnapshot());
        appendLabeledSection(builder, "Resume snapshot", session.getResumeSnapshot());
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildCoverLetterGenerationInput(InterviewSession session) {
        StringBuilder builder = new StringBuilder();
        appendLabeledSection(builder, "Session title", session.getTitle());
        appendLabeledSection(builder, "Target position", session.getPositionTitle());
        appendLabeledSection(builder, "Interview mode", resolveInterviewMode(session.getMode()).name());
        appendLabeledSection(builder, "Application document snapshot", session.getApplicationDocumentSnapshot());
        appendLabeledSection(builder, "Cover letter snapshot", session.getCoverLetterSnapshot());
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildJobGenerationInput(
            InterviewSession session,
            List<String> generatedQuestions,
            List<String> rejectedQuestions,
            int questionIndex,
            int totalQuestionCount,
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty
    ) {
        StringBuilder builder = new StringBuilder();
        List<String> materialHighlights = buildMaterialHighlights(session);
        appendLabeledSection(builder, "Session title", session.getTitle());
        appendLabeledSection(builder, "Target position", session.getPositionTitle());
        appendLabeledSection(builder, "Interview mode", mode.name());
        appendLabeledSection(builder, "Planned question number", questionIndex + " / " + totalQuestionCount);
        appendLabeledSection(builder, "Question category", category.name());
        appendLabeledSection(builder, "Question difficulty", difficulty.name());
        appendLabeledSection(builder, "Mode guidance", buildModeGuide(mode, category, difficulty, questionIndex, totalQuestionCount));
        if (!materialHighlights.isEmpty()) {
            appendLabeledSection(builder, "Candidate material highlights", String.join("\n", materialHighlights));
            appendLabeledSection(builder, "Priority rule", "Prefer a question anchored to the candidate's submitted materials before asking a generic question.");
        }
        appendLabeledSection(builder, "Job posting snapshot", session.getJobPostingSnapshot());
        if (!generatedQuestions.isEmpty()) {
            appendLabeledSection(builder, "Already generated questions", String.join("\n", generatedQuestions));
            appendLabeledSection(builder, "Generation rule", "Do not repeat or paraphrase the already generated questions.");
            appendLabeledSection(builder, "Avoid semantic overlap", "Each new question must test a different competency and context from prior questions.");
        }
        if (!rejectedQuestions.isEmpty()) {
            appendLabeledSection(builder, "Rejected candidate questions", String.join("\n", rejectedQuestions));
            appendLabeledSection(builder, "Retry rule", "Do not reuse or paraphrase any rejected candidate question.");
        }
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String generateAiQuestionWithRetry(
            InterviewSession session,
            List<String> generatedQuestions,
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty,
            int questionIndex,
            int totalQuestionCount
    ) {
        RuntimeException lastException = null;
        List<String> rejectedQuestions = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_AI_GENERATION_RETRIES; attempt++) {
            try {
                InterviewQuestionGenerationContext context = new InterviewQuestionGenerationContext(
                        mode.name(),
                        category.name(),
                        difficulty.name(),
                        questionIndex,
                        totalQuestionCount,
                        buildModeGuide(mode, category, difficulty, questionIndex, totalQuestionCount),
                        buildMaterialHighlights(session),
                        List.copyOf(generatedQuestions)
                );
                String questionText = aiService.generateInterviewQuestion(
                        buildResumeGenerationInput(session),
                        buildCoverLetterGenerationInput(session),
                        buildJobGenerationInput(session, generatedQuestions, rejectedQuestions, questionIndex, totalQuestionCount, mode, category, difficulty),
                        context,
                        List.of()
                );
                if (!StringUtils.hasText(questionText)) {
                    throw new IllegalStateException("AI question text is empty.");
                }

                String normalizedQuestionText = questionText.trim();
                if (containsSimilarQuestion(generatedQuestions, normalizedQuestionText)) {
                    rejectedQuestions.add(normalizedQuestionText);
                    throw new IllegalStateException("AI returned a duplicated or similar interview question.");
                }
                return normalizedQuestionText;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < MAX_AI_GENERATION_RETRIES) {
                    log.debug(
                            "[Interview] Retrying AI question generation. sessionId={}, questionIndex={}, nextAttempt={}/{}",
                            session.getId(),
                            questionIndex,
                            attempt + 1,
                            MAX_AI_GENERATION_RETRIES
                    );
                }
            }
        }

        throw lastException == null
                ? new IllegalStateException("Failed to generate interview question.")
                : lastException;
    }

    private String selectQuestionCandidate(
            InterviewSession session,
            List<String> candidates,
            List<String> generatedQuestions,
            int index,
            InterviewQuestionDifficulty difficulty
    ) {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Fallback question catalog is empty.");
        }

        int seed = Long.hashCode(session.getId() == null ? index : session.getId())
                + safeHash(session.getTitle())
                + safeHash(session.getPositionTitle())
                + difficulty.ordinal() * 17
                + index * 13;
        int startIndex = Math.floorMod(seed, candidates.size());

        for (int offset = 0; offset < candidates.size(); offset++) {
            String candidate = candidates.get((startIndex + offset) % candidates.size());
            if (!containsSimilarQuestion(generatedQuestions, candidate)) {
                return candidate;
            }
        }

        return candidates.get(startIndex);
    }

    private InterviewMode resolveInterviewMode(InterviewMode mode) {
        return mode == null ? InterviewMode.COMPREHENSIVE : mode;
    }

    private String buildModeGuide(
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty,
            int questionIndex,
            int totalQuestionCount
    ) {
        String jobGuide = category == InterviewQuestionCategory.FRONTEND
                ? "Focus on frontend UI architecture, rendering, browser behavior, performance, accessibility, and collaboration with design."
                : "Focus on backend API design, data consistency, transactions, performance, scalability, operations, and collaboration with product and frontend.";
        String difficultyGuide = switch (difficulty) {
            case EASY -> "Keep the question accessible and confirm fundamentals or a concrete first-hand example.";
            case MEDIUM -> "Require applied experience, problem solving steps, tradeoffs, or collaboration details.";
            case HARD -> "Probe architecture, ambiguous tradeoffs, leadership judgment, or deep troubleshooting.";
        };
        String modeGuide = switch (mode) {
            case BEHAVIORAL -> "Ask about collaboration, conflict, failure, motivation, ownership, growth, and decision making. Do not drift into textbook theory.";
            case TECHNICAL -> "Ask technical questions about domain knowledge, troubleshooting, system design, performance, and CS fundamentals. Require concrete reasoning.";
            case COMPREHENSIVE -> questionIndex % 2 == 1
                    ? "This is a mixed interview. Prefer a behavioral or experience-driven question tied to technical work for this slot."
                    : "This is a mixed interview. Prefer a technical or troubleshooting question tied to real project context for this slot.";
            case RESUME_BASED -> "Anchor the question to resume, cover letter, project claims, metrics, ownership, and consistency of the candidate's submitted materials.";
        };
        return modeGuide + " " + jobGuide + " " + difficultyGuide
                + " Question slot is " + questionIndex + " out of " + totalQuestionCount + ".";
    }

    private List<String> buildMaterialHighlights(InterviewSession session) {
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        appendMaterialHighlights(highlights, session.getResumeSnapshot());
        appendMaterialHighlights(highlights, session.getCoverLetterSnapshot());
        appendMaterialHighlights(highlights, session.getApplicationDocumentSnapshot());
        appendMaterialHighlights(highlights, session.getJobPostingSnapshot());
        return highlights.stream()
                .limit(6)
                .toList();
    }

    private void appendMaterialHighlights(LinkedHashSet<String> highlights, String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return;
        }

        String[] lines = snapshot.split("\\n");
        for (String rawLine : lines) {
            if (isMetadataLine(rawLine)) {
                continue;
            }
            String normalizedLine = stripSnapshotLabel(rawLine);
            if (!StringUtils.hasText(normalizedLine)) {
                continue;
            }

            String[] fragments = normalizedLine.split("(?<=[.!?])\\s+|,\\s+");
            for (String fragment : fragments) {
                String highlight = normalizeMaterialHighlight(fragment);
                if (StringUtils.hasText(highlight)) {
                    highlights.add(highlight);
                }
            }
        }
    }

    private boolean isMetadataLine(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("document title:")
                || normalized.startsWith("resume title:")
                || normalized.startsWith("cover letter title:")
                || normalized.startsWith("original file name:")
                || normalized.startsWith("file url:")
                || normalized.startsWith("company:")
                || normalized.startsWith("position:");
    }

    private String stripSnapshotLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        int delimiterIndex = trimmed.indexOf(':');
        if (delimiterIndex >= 0 && delimiterIndex < trimmed.length() - 1) {
            return trimmed.substring(delimiterIndex + 1).trim();
        }
        return trimmed;
    }

    private String normalizeMaterialHighlight(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        String lowercase = normalized.toLowerCase(Locale.ROOT);
        if (normalized.length() < 12 || normalized.length() > 150) {
            return null;
        }
        if (lowercase.startsWith("http") || lowercase.contains("[truncated]")) {
            return null;
        }
        if (lowercase.startsWith("backend engineer") || lowercase.startsWith("frontend engineer")) {
            return null;
        }
        return normalized;
    }

    private String buildContextualFallbackQuestion(
            List<String> materialHighlights,
            List<String> generatedQuestions,
            int index,
            InterviewMode mode,
            InterviewQuestionDifficulty difficulty
    ) {
        if (materialHighlights.isEmpty()) {
            return null;
        }

        for (int offset = 0; offset < materialHighlights.size(); offset++) {
            String highlight = materialHighlights.get(Math.floorMod(index - 1 + offset, materialHighlights.size()));
            String candidate = buildDiversifiedMaterialQuestion(highlight, mode, difficulty, index + offset);
            if (!containsSimilarQuestion(generatedQuestions, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String buildMaterialQuestion(String highlight, InterviewMode mode, InterviewQuestionDifficulty difficulty) {
        String quotedHighlight = "'" + highlight + "'";
        return switch (mode) {
            case BEHAVIORAL -> switch (difficulty) {
                case EASY -> quotedHighlight + " 경험에서 맡았던 역할과 협업 방식을 설명해 주세요.";
                case MEDIUM -> quotedHighlight + " 경험에서 문제 상황이 생겼을 때 어떻게 조율하고 해결했는지 설명해 주세요.";
                case HARD -> quotedHighlight + " 경험에서 본인이 주도적으로 내린 판단과 그 근거를 설명해 주세요.";
            };
            case TECHNICAL -> switch (difficulty) {
                case EASY -> quotedHighlight + "와 관련해 실제로 사용한 기술과 구현 방식을 설명해 주세요.";
                case MEDIUM -> quotedHighlight + "를 수행하면서 겪은 기술적 문제와 해결 과정을 설명해 주세요.";
                case HARD -> quotedHighlight + "에서 기술 선택이나 아키텍처를 결정할 때 고려한 트레이드오프를 설명해 주세요.";
            };
            case RESUME_BASED -> switch (difficulty) {
                case EASY -> "지원 자료에 적힌 " + quotedHighlight + " 내용에서 직접 기여한 부분을 설명해 주세요.";
                case MEDIUM -> "지원 자료에 적힌 " + quotedHighlight + " 내용이 실제 성과로 이어진 과정을 설명해 주세요.";
                case HARD -> "지원 자료에 적힌 " + quotedHighlight + " 성과나 주장에 대해 어떤 지표와 근거로 검증할 수 있는지 설명해 주세요.";
            };
            case COMPREHENSIVE -> switch (difficulty) {
                case EASY -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 소개하고, 이 경험이 지원 직무와 어떻게 연결되는지 설명해 주세요.";
                case MEDIUM -> "지원 자료에 적힌 " + quotedHighlight + " 경험을 바탕으로 문제 해결 과정과 협업 방식을 함께 설명해 주세요.";
                case HARD -> "지원 자료에 적힌 " + quotedHighlight + " 경험에서 기술적 판단과 비즈니스 판단을 어떻게 균형 있게 가져갔는지 설명해 주세요.";
            };
        };
    }

    private String buildDiversifiedMaterialQuestion(
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
        return focuses.get(Math.floorMod(questionSeed - 1, focuses.size()));
    }

    private boolean containsSimilarQuestion(List<String> existingQuestions, String candidate) {
        return existingQuestions.stream().anyMatch(existing -> isSimilarQuestion(existing, candidate));
    }

    private boolean isSimilarQuestion(String left, String right) {
        String normalizedLeft = normalizeQuestion(left);
        String normalizedRight = normalizeQuestion(right);
        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return true;
        }

        Set<String> leftTokens = tokenizeQuestion(left);
        Set<String> rightTokens = tokenizeQuestion(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }

        long intersection = leftTokens.stream().filter(rightTokens::contains).count();
        int union = leftTokens.size() + rightTokens.size() - (int) intersection;
        double similarity = union == 0 ? 0.0 : (double) intersection / union;
        return similarity >= QUESTION_SIMILARITY_THRESHOLD;
    }

    private String normalizeQuestion(String questionText) {
        if (!StringUtils.hasText(questionText)) {
            return "";
        }
        return questionText.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private Set<String> tokenizeQuestion(String questionText) {
        if (!StringUtils.hasText(questionText)) {
            return Set.of();
        }
        return Set.copyOf(List.of(questionText.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .split("\\s+")).stream()
                .filter(token -> token.length() >= 2)
                .filter(token -> !QUESTION_STOP_WORDS.contains(token))
                .toList());
    }

    private int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private void appendLabeledSection(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim().replace("\r\n", "\n");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "\n[truncated]";
    }

    private InterviewQuestionResponse toQuestionResponse(InterviewQuestion question) {
        InterviewAnswer answer = question.getAnswer();
        String answerText = answer == null ? null : answer.getAnswerText();
        boolean answered = StringUtils.hasText(answerText);
        return new InterviewQuestionResponse(
                question.getId(),
                question.getSequenceNumber(),
                question.getQuestionText(),
                answerText,
                answer == null ? null : answer.getAudioUrl(),
                answered,
                answered ? answerText.length() : 0,
                answer == null ? null : answer.getUpdatedAt(),
                question.getCreatedAt()
        );
    }

    private InterviewAnswerResponse toAnswerResponse(InterviewAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new InterviewAnswerResponse(
                answer.getId(),
                answer.getAnswerText(),
                answer.getAudioUrl(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }

    private InterviewFeedbackResponse toFeedbackResponse(InterviewFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return new InterviewFeedbackResponse(
                feedback.getId(),
                feedback.getRelevanceScore(),
                feedback.getLogicScore(),
                feedback.getSpecificityScore(),
                feedback.getOverallScore(),
                feedback.getWeakPoints(),
                feedback.getImprovements(),
                feedback.getRecommendedAnswer(),
                feedback.getCreatedAt()
        );
    }

    private int calculateCompletionRate(int totalQuestions, int answeredQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return Math.round(answeredQuestions * 100.0f / totalQuestions);
    }

    private long calculateDurationMinutes(InterviewSession session) {
        LocalDateTime endTime = session.getEndedAt() == null ? LocalDateTime.now() : session.getEndedAt();
        return Math.max(0, Duration.between(session.getStartedAt(), endTime).toMinutes());
    }

    private String buildResultSummary(InterviewFeedbackResponse feedback, int totalQuestions, int answeredQuestions) {
        int completionRate = calculateCompletionRate(totalQuestions, answeredQuestions);
        int overallScore = feedback == null || feedback.overallScore() == null ? 0 : feedback.overallScore();
        if (answeredQuestions == 0) {
            return "아직 저장된 답변이 없습니다. 답변을 저장하면 결과 요약과 개선 방향을 함께 보여드립니다.";
        }
        return "전체 " + totalQuestions + "개 질문 중 " + answeredQuestions + "개에 답변했습니다. "
                + "완료율은 " + completionRate + "%이며 현재 종합 점수는 " + overallScore + "점입니다.";
    }

    private List<String> buildHighlights(
            InterviewSession session,
            InterviewFeedbackResponse feedback,
            int totalQuestions,
            int answeredQuestions
    ) {
        List<String> highlights = new ArrayList<>();
        highlights.add("전체 " + totalQuestions + "개 질문 중 " + answeredQuestions + "개에 답변했습니다.");
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            highlights.add("면접이 정상적으로 종료되었습니다. 결과를 바탕으로 다음 연습 방향을 확인해 보세요.");
        } else {
            highlights.add("아직 답변하지 않은 질문이 남아 있습니다. 면접을 마무리하면 더 정확한 결과를 볼 수 있습니다.");
        }
        if (feedback != null && feedback.overallScore() != null) {
            highlights.add("현재 종합 점수는 " + feedback.overallScore() + "점입니다.");
        }
        if (feedback != null && StringUtils.hasText(feedback.improvements())) {
            highlights.add(feedback.improvements());
        }
        return highlights;
    }

    private List<InterviewLearningRecommendationResponse> buildLearningRecommendations(
            InterviewFeedbackResponse feedback,
            int unansweredQuestions
    ) {
        List<InterviewLearningRecommendationResponse> recommendations = new ArrayList<>();
        if (feedback == null) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "답변 연습",
                    "아직 분석할 답변이 없어 학습 추천을 만들 수 없습니다.",
                    "질문에 답변을 저장한 뒤 결과 화면에서 다시 추천 학습을 확인해 보세요."
            ));
            return recommendations;
        }
        if (feedback.logicScore() != null && feedback.logicScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "답변 구조",
                    "논리성 점수가 낮아 답변 흐름을 더 정리할 필요가 있습니다.",
                    "STAR 방식으로 상황, 행동, 결과를 나누어 말하는 연습을 해보세요."
            ));
        }
        if (feedback.relevanceScore() != null && feedback.relevanceScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "직무 연관성",
                    "답변이 지원 직무와 직접 연결되는 근거가 부족해 보입니다.",
                    "직무 요구사항과 연결되는 경험을 한 가지씩 다시 정리해 보세요."
            ));
        }
        if (feedback.specificityScore() != null && feedback.specificityScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "구체성 강화",
                    "답변에 성과 수치나 구체적인 사례가 더 필요합니다.",
                    "수치, 일정, 사용 기술, 성과 결과를 포함해 답변을 다시 써 보세요."
            ));
        }
        if (unansweredQuestions > 0) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "미응답 보완",
                    "아직 답변하지 않은 질문이 남아 있습니다.",
                    "남은 질문까지 답변을 채운 뒤 전체 결과를 다시 확인해 보세요."
            ));
        }
        if (recommendations.isEmpty()) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "심화 연습",
                    "전반적으로 안정적인 답변을 보여 주고 있습니다.",
                    "다음 연습에서는 의사결정 이유와 대안 비교까지 포함해 답변을 고도화해 보세요."
            ));
        }
        return recommendations;
    }
    private record AnswerAnalysis(
            int answeredCount,
            int averageLength,
            int structureHits,
            int metricHits,
            int keywordHits
    ) {
    }
    private record QuestionGenerationResult(
            List<InterviewQuestion> questions,
            String source,
            boolean fallbackUsed,
            String message
    ) {
    }
}
