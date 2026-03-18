package com.aimentor.domain.profile.repository;

import com.aimentor.domain.profile.entity.CoverLetter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {

    List<CoverLetter> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

    List<CoverLetter> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<CoverLetter> findByIdAndUserId(Long id, Long userId);
}
