package com.aimentor.domain.profile.repository;

import com.aimentor.domain.profile.entity.JobPosting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    List<JobPosting> findByPositionTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCaseOrderByUpdatedAtDesc(String positionKeyword, String companyKeyword);

    List<JobPosting> findAllByOrderByUpdatedAtDesc();

    List<JobPosting> findByUserIdAndPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

    List<JobPosting> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<JobPosting> findByIdAndUserId(Long id, Long userId);
}
