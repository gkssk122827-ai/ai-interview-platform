package com.aimentor.domain.profile.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.profile.dto.request.ResumeUpsertRequest;
import com.aimentor.domain.profile.dto.response.ResumeResponse;
import com.aimentor.domain.profile.entity.Resume;
import com.aimentor.domain.profile.repository.ResumeRepository;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles resume CRUD scoped to the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ResumeResponse create(Long userId, ResumeUpsertRequest request) {
        User user = getUser(userId);
        Resume resume = Resume.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .fileUrl(request.fileUrl())
                .build();
        return toResponse(resumeRepository.save(resume));
    }

    public List<ResumeResponse> list(Long userId, String keyword) {
        List<Resume> resumes = keyword == null || keyword.isBlank()
                ? resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                : resumeRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, keyword);
        return resumes.stream().map(this::toResponse).toList();
    }

    public ResumeResponse get(Long userId, Long resumeId) {
        return toResponse(getOwnedResume(userId, resumeId));
    }

    @Transactional
    public ResumeResponse update(Long userId, Long resumeId, ResumeUpsertRequest request) {
        Resume resume = getOwnedResume(userId, resumeId);
        resume.update(request.title(), request.content(), request.fileUrl());
        return toResponse(resume);
    }

    @Transactional
    public void delete(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        resumeRepository.delete(resume);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private Resume getOwnedResume(Long userId, Long resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND", "Resume not found."));
    }

    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUser().getId(),
                resume.getTitle(),
                resume.getContent(),
                resume.getFileUrl(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
