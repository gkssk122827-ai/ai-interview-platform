package com.aimentor.domain.interview.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_question_id", unique = true)
    private InterviewQuestion interviewQuestion;

    @Column(nullable = false, length = 5000)
    private String answerText;

    @Column(length = 500)
    private String audioUrl;

    @Builder
    public InterviewAnswer(InterviewQuestion interviewQuestion, String answerText, String audioUrl) {
        this.interviewQuestion = interviewQuestion;
        this.answerText = answerText;
        this.audioUrl = audioUrl;
    }

    public void update(String answerText, String audioUrl) {
        this.answerText = answerText;
        this.audioUrl = audioUrl;
    }
}
