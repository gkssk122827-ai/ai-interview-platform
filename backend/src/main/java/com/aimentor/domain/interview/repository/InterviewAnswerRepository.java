package com.aimentor.domain.interview.repository;

import com.aimentor.domain.interview.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
}
