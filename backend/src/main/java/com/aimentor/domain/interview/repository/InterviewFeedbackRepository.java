package com.aimentor.domain.interview.repository;

import com.aimentor.domain.interview.entity.InterviewFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {

    Optional<InterviewFeedback> findByInterviewSessionId(Long interviewSessionId);
}
