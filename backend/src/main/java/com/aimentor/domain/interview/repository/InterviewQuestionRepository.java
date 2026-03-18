package com.aimentor.domain.interview.repository;

import com.aimentor.domain.interview.entity.InterviewQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    Optional<InterviewQuestion> findByIdAndInterviewSessionIdAndInterviewSessionUserId(
            Long id,
            Long interviewSessionId,
            Long userId
    );
}
