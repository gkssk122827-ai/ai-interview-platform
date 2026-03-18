package com.aimentor.domain.interview.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import com.aimentor.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String positionTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewMode mode;

    private Long applicationDocumentId;

    private Long resumeId;

    private Long coverLetterId;

    private Long jobPostingId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String applicationDocumentSnapshot;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resumeSnapshot;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String coverLetterSnapshot;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jobPostingSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @OneToOne(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private InterviewFeedback feedback;

    @Builder
    public InterviewSession(
            User user,
            String title,
            String positionTitle,
            InterviewMode mode,
            Long applicationDocumentId,
            Long resumeId,
            Long coverLetterId,
            Long jobPostingId,
            String applicationDocumentSnapshot,
            String resumeSnapshot,
            String coverLetterSnapshot,
            String jobPostingSnapshot,
            InterviewSessionStatus status,
            LocalDateTime startedAt
    ) {
        this.user = user;
        this.title = title;
        this.positionTitle = positionTitle;
        this.mode = mode;
        this.applicationDocumentId = applicationDocumentId;
        this.resumeId = resumeId;
        this.coverLetterId = coverLetterId;
        this.jobPostingId = jobPostingId;
        this.applicationDocumentSnapshot = applicationDocumentSnapshot;
        this.resumeSnapshot = resumeSnapshot;
        this.coverLetterSnapshot = coverLetterSnapshot;
        this.jobPostingSnapshot = jobPostingSnapshot;
        this.status = status;
        this.startedAt = startedAt;
    }

    public void addQuestion(InterviewQuestion question) {
        this.questions.add(question);
    }

    public void end() {
        this.status = InterviewSessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void assignFeedback(InterviewFeedback feedback) {
        this.feedback = feedback;
    }

    public int getTotalQuestionCount() {
        return questions.size();
    }

    public int getAnsweredQuestionCount() {
        return (int) questions.stream()
                .filter(question -> question.getAnswer() != null
                        && question.getAnswer().getAnswerText() != null
                        && !question.getAnswer().getAnswerText().isBlank())
                .count();
    }
}
