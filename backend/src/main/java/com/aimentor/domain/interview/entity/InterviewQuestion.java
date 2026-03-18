package com.aimentor.domain.interview.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @OneToOne(mappedBy = "interviewQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private InterviewAnswer answer;

    @Builder
    public InterviewQuestion(InterviewSession interviewSession, Integer sequenceNumber, String questionText) {
        this.interviewSession = interviewSession;
        this.sequenceNumber = sequenceNumber;
        this.questionText = questionText;
    }

    public void assignAnswer(InterviewAnswer answer) {
        this.answer = answer;
    }
}
