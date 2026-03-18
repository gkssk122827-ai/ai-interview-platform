package com.aimentor.domain.profile.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.profile.dto.request.CoverLetterUpsertRequest;
import com.aimentor.domain.profile.dto.response.CoverLetterResponse;
import com.aimentor.domain.profile.entity.CoverLetter;
import com.aimentor.domain.profile.repository.CoverLetterRepository;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles cover-letter CRUD scoped to the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final UserRepository userRepository;

    public CoverLetterService(CoverLetterRepository coverLetterRepository, UserRepository userRepository) {
        this.coverLetterRepository = coverLetterRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CoverLetterResponse create(Long userId, CoverLetterUpsertRequest request) {
        User user = getUser(userId);
        CoverLetter coverLetter = CoverLetter.builder()
                .user(user)
                .title(request.title())
                .companyName(request.companyName())
                .content(request.content())
                .build();
        return toResponse(coverLetterRepository.save(coverLetter));
    }

    public List<CoverLetterResponse> list(Long userId, String keyword) {
        List<CoverLetter> coverLetters = keyword == null || keyword.isBlank()
                ? coverLetterRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                : coverLetterRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, keyword);
        return coverLetters.stream().map(this::toResponse).toList();
    }

    public CoverLetterResponse get(Long userId, Long coverLetterId) {
        return toResponse(getOwnedCoverLetter(userId, coverLetterId));
    }

    @Transactional
    public CoverLetterResponse update(Long userId, Long coverLetterId, CoverLetterUpsertRequest request) {
        CoverLetter coverLetter = getOwnedCoverLetter(userId, coverLetterId);
        coverLetter.update(request.title(), request.companyName(), request.content());
        return toResponse(coverLetter);
    }

    @Transactional
    public void delete(Long userId, Long coverLetterId) {
        CoverLetter coverLetter = getOwnedCoverLetter(userId, coverLetterId);
        coverLetterRepository.delete(coverLetter);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private CoverLetter getOwnedCoverLetter(Long userId, Long coverLetterId) {
        return coverLetterRepository.findByIdAndUserId(coverLetterId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COVER_LETTER_NOT_FOUND", "Cover letter not found."));
    }

    private CoverLetterResponse toResponse(CoverLetter coverLetter) {
        return new CoverLetterResponse(
                coverLetter.getId(),
                coverLetter.getUser().getId(),
                coverLetter.getTitle(),
                coverLetter.getCompanyName(),
                coverLetter.getContent(),
                coverLetter.getCreatedAt(),
                coverLetter.getUpdatedAt()
        );
    }
}
