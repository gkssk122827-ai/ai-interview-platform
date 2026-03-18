package com.aimentor.domain.profile.repository;

import com.aimentor.domain.profile.entity.ApplicationDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    List<ApplicationDocument> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

    List<ApplicationDocument> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ApplicationDocument> findByIdAndUserId(Long id, Long userId);
}
