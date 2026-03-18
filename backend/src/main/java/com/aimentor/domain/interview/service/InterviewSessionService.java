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
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import com.aimentor.external.ai.AiService;
import com.aimentor.external.ai.ConversationTurnDto;
import com.aimentor.external.ai.dto.FeedbackDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final UserRepository userRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ResumeRepository resumeRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final JobPostingRepository jobPostingRepository;
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
                    "AI-generated interview questions are ready."
            );
        } catch (RuntimeException ex) {
            log.warn("[Interview] Falling back to mock question generation. sessionId={}", session.getId(), ex);
            return new QuestionGenerationResult(
                    buildMockQuestions(session, questionCount),
                    "MOCK",
                    true,
                    "AI question generation failed, so the session started with fallback questions."
            );
        }
    }

    private List<InterviewQuestion> buildAiQuestions(InterviewSession session, int questionCount) {
        List<InterviewQuestion> questions = new ArrayList<>();
        List<String> generatedQuestions = new ArrayList<>();

        for (int index = 1; index <= questionCount; index++) {
            String questionText = aiService.generateInterviewQuestion(
                    buildResumeGenerationInput(session),
                    buildCoverLetterGenerationInput(session),
                    buildJobGenerationInput(session, generatedQuestions, index, questionCount),
                    List.of()
            );

            if (!StringUtils.hasText(questionText)) {
                throw new IllegalStateException("AI question text is empty.");
            }

            String normalizedQuestionText = questionText.trim();
            if (generatedQuestions.stream().anyMatch(existing -> existing.equalsIgnoreCase(normalizedQuestionText))) {
                throw new IllegalStateException("AI returned a duplicated interview question.");
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
        for (int index = 1; index <= questionCount; index++) {
            questions.add(InterviewQuestion.builder()
                    .interviewSession(session)
                    .sequenceNumber(index)
                    .questionText(buildMockQuestionText(session, index))
                    .build());
        }
        return questions;
    }

    private String buildMockQuestionText(InterviewSession session, int index) {
        return switch (index) {
            case 1 -> session.getPositionTitle() + " 직무에 지원한 이유와 본인의 강점을 소개해 주세요.";
            case 2 -> "최근 경험 중 " + session.getPositionTitle() + " 직무와 가장 관련 있는 프로젝트를 설명해 주세요.";
            case 3 -> "협업이나 문제 해결 과정에서 본인이 주도적으로 기여한 사례를 말해 주세요.";
            default -> "지원 직무와 연결되는 경험이나 역량을 구체적인 사례와 함께 설명해 주세요. (" + index + ")";
        };
    }

    private FeedbackDto buildPendingFeedback() {
        return new FeedbackDto(
                0,
                0,
                0,
                0,
                "No weak points yet. Start answering questions.",
                "Provide concrete examples and structure your answers clearly.",
                "Use the STAR format and mention measurable impact."
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
        long answeredCount = session.getAnsweredQuestionCount();
        int relevanceScore = (int) Math.min(100, 55 + answeredCount * 10);
        int logicScore = (int) Math.min(100, 50 + answeredCount * 12);
        int specificityScore = (int) Math.min(100, 45 + answeredCount * 15);
        int overallScore = Math.round((relevanceScore + logicScore + specificityScore) / 3.0f);

        return new FeedbackDto(
                logicScore,
                relevanceScore,
                specificityScore,
                overallScore,
                "Fallback weak points: some answers still need clearer evidence and tighter structure.",
                "Fallback improvements: explain your role, actions, and outcomes in a more structured way.",
                "Fallback recommended answer: briefly explain the context, your action, the result, and what you learned."
        );
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
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildResumeSnapshot(Resume resume) {
        if (resume == null) {
            return null;
        }
        return limitText("Resume title: " + resume.getTitle() + "\nResume content: " + resume.getContent(), MAX_CONTEXT_LENGTH);
    }

    private String buildResumeSnapshotFromApplicationDocument(ApplicationDocument applicationDocument) {
        if (applicationDocument == null || !StringUtils.hasText(applicationDocument.getResumeText())) {
            return null;
        }
        return limitText(
                "Resume title: " + applicationDocument.getTitle() + "\nResume content: " + applicationDocument.getResumeText(),
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
        if (applicationDocument == null || !StringUtils.hasText(applicationDocument.getCoverLetterText())) {
            return null;
        }
        return limitText(
                "Cover letter title: " + applicationDocument.getTitle()
                        + "\nContent: " + applicationDocument.getCoverLetterText(),
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
        appendLabeledSection(builder, "Application document snapshot", session.getApplicationDocumentSnapshot());
        appendLabeledSection(builder, "Resume snapshot", session.getResumeSnapshot());
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildCoverLetterGenerationInput(InterviewSession session) {
        StringBuilder builder = new StringBuilder();
        appendLabeledSection(builder, "Session title", session.getTitle());
        appendLabeledSection(builder, "Target position", session.getPositionTitle());
        appendLabeledSection(builder, "Application document snapshot", session.getApplicationDocumentSnapshot());
        appendLabeledSection(builder, "Cover letter snapshot", session.getCoverLetterSnapshot());
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
    }

    private String buildJobGenerationInput(
            InterviewSession session,
            List<String> generatedQuestions,
            int questionIndex,
            int totalQuestionCount
    ) {
        StringBuilder builder = new StringBuilder();
        appendLabeledSection(builder, "Session title", session.getTitle());
        appendLabeledSection(builder, "Target position", session.getPositionTitle());
        appendLabeledSection(builder, "Planned question number", questionIndex + " / " + totalQuestionCount);
        appendLabeledSection(builder, "Job posting snapshot", session.getJobPostingSnapshot());
        if (!generatedQuestions.isEmpty()) {
            appendLabeledSection(builder, "Already generated questions", String.join("\n", generatedQuestions));
            appendLabeledSection(builder, "Generation rule", "Do not repeat the already generated questions.");
        }
        return limitText(builder.toString(), MAX_CONTEXT_LENGTH);
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
            return "No answers were saved in this session. Save at least one answer to receive a meaningful analysis.";
        }
        return "This session covered " + totalQuestions + " questions, with " + answeredQuestions + " answers saved. "
                + "Completion reached " + completionRate + "% and the overall feedback score is " + overallScore + ".";
    }

    private List<String> buildHighlights(
            InterviewSession session,
            InterviewFeedbackResponse feedback,
            int totalQuestions,
            int answeredQuestions
    ) {
        List<String> highlights = new ArrayList<>();
        highlights.add("Answered " + answeredQuestions + " out of " + totalQuestions + " questions.");
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            highlights.add("The session was completed and the final feedback has been stored.");
        } else {
            highlights.add("The session is still ongoing. Final feedback can improve after completion.");
        }
        if (feedback != null && feedback.overallScore() != null) {
            highlights.add("Current overall score: " + feedback.overallScore() + ".");
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
                    "Answer completion",
                    "There is not enough feedback yet because the session has no saved answers.",
                    "Finish at least one answer before moving to follow-up learning."
            ));
            return recommendations;
        }

        if (feedback.logicScore() != null && feedback.logicScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "Answer structure",
                    "Logic score is below the target range.",
                    "Practice STAR-based speaking drills and focus on sequencing context, action, and result."
            ));
        }
        if (feedback.relevanceScore() != null && feedback.relevanceScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "Role alignment",
                    "Relevance score suggests your answers are not consistently tied back to the target role.",
                    "Review the job posting and rewrite each answer to connect directly to the required skills."
            ));
        }
        if (feedback.specificityScore() != null && feedback.specificityScore() < 75) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "Evidence and metrics",
                    "Specificity score shows that examples can be more concrete.",
                    "Add measurable outcomes, ownership scope, and technical details to each answer."
            ));
        }
        if (unansweredQuestions > 0) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "Completion",
                    "Some interview questions were left unanswered.",
                    "Return to the session or rehearse short draft answers for the unanswered questions first."
            ));
        }
        if (recommendations.isEmpty()) {
            recommendations.add(new InterviewLearningRecommendationResponse(
                    "Advanced refinement",
                    "Core scores are stable across the session.",
                    "Move on to higher-difficulty mock interviews and refine concise delivery."
            ));
        }
        return recommendations;
    }

    private record QuestionGenerationResult(
            List<InterviewQuestion> questions,
            String source,
            boolean fallbackUsed,
            String message
    ) {
    }
}
