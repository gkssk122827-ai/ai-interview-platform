package com.aimentor.domain.profile.repository;

import com.aimentor.domain.profile.entity.Resume;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

    List<Resume> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);
}
