package com.aimentor.domain.profile.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.profile.dto.request.ApplicationDocumentUpsertRequest;
import com.aimentor.domain.profile.dto.response.ApplicationDocumentResponse;
import com.aimentor.domain.profile.entity.ApplicationDocument;
import com.aimentor.domain.profile.repository.ApplicationDocumentRepository;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles unified application-document CRUD and file storage scoped to the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationDocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 12000;
    private static final Logger log = LoggerFactory.getLogger(ApplicationDocumentService.class);

    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final UserRepository userRepository;
    private final Path uploadRootPath;

    public ApplicationDocumentService(
            ApplicationDocumentRepository applicationDocumentRepository,
            UserRepository userRepository,
            @Value("${app.profile-document.upload-dir:uploads/profile-documents}") String uploadRootPath
    ) {
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.userRepository = userRepository;
        this.uploadRootPath = Path.of(uploadRootPath).toAbsolutePath().normalize();
    }

    @Transactional
    public ApplicationDocumentResponse create(Long userId, ApplicationDocumentUpsertRequest request, MultipartFile file) {
        User user = getUser(userId);
        validateContent(request.getResumeText(), request.getCoverLetterText(), file, false);

        ApplicationDocument applicationDocument = ApplicationDocument.builder()
                .user(user)
                .title(normalizeRequired(request.getTitle()))
                .resumeText(normalizeOptional(request.getResumeText()))
                .coverLetterText(normalizeOptional(request.getCoverLetterText()))
                .build();

        ApplicationDocument savedDocument = applicationDocumentRepository.save(applicationDocument);
        if (hasFile(file)) {
            applyStoredFile(savedDocument, storeFile(savedDocument.getId(), file));
        }
        return toResponse(savedDocument);
    }

    public List<ApplicationDocumentResponse> list(Long userId, String keyword) {
        log.info("[ProfileDocuments] Service start: list userId={}, keyword={}", userId, keyword);
        log.info("[ProfileDocuments] Repository call start: list");
        List<ApplicationDocument> documents = keyword == null || keyword.isBlank()
                ? applicationDocumentRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                : applicationDocumentRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, keyword);
        log.info("[ProfileDocuments] Repository returned count={}", documents.size());
        List<ApplicationDocumentResponse> response = documents.stream().map(this::toResponse).toList();
        log.info("[ProfileDocuments] Service complete: list count={}", response.size());
        return response;
    }

    public ApplicationDocumentResponse get(Long userId, Long documentId) {
        return toResponse(getOwnedDocument(userId, documentId));
    }

    public ApplicationDocumentFileDownload loadFile(Long userId, Long documentId) {
        ApplicationDocument applicationDocument = getOwnedDocument(userId, documentId);
        if (!StringUtils.hasText(applicationDocument.getStoredFilePath())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_DOCUMENT_FILE_NOT_FOUND", "Profile document file not found.");
        }

        Path filePath = uploadRootPath.resolve(applicationDocument.getStoredFilePath()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_DOCUMENT_FILE_NOT_FOUND", "Profile document file not found.");
            }

            String contentType = Files.probeContentType(filePath);
            return new ApplicationDocumentFileDownload(
                    resource,
                    applicationDocument.getOriginalFileName(),
                    StringUtils.hasText(contentType) ? contentType : "application/octet-stream"
            );
        } catch (MalformedURLException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_DOCUMENT_FILE_READ_ERROR", "Failed to read profile document file.");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_DOCUMENT_FILE_READ_ERROR", "Failed to read profile document file.");
        }
    }

    @Transactional
    public ApplicationDocumentResponse update(Long userId, Long documentId, ApplicationDocumentUpsertRequest request, MultipartFile file) {
        ApplicationDocument applicationDocument = getOwnedDocument(userId, documentId);
        String nextResumeText = request.getResumeText() == null
                ? applicationDocument.getResumeText()
                : normalizeOptional(request.getResumeText());
        String nextCoverLetterText = request.getCoverLetterText() == null
                ? applicationDocument.getCoverLetterText()
                : normalizeOptional(request.getCoverLetterText());

        validateContent(nextResumeText, nextCoverLetterText, file, hasStoredFile(applicationDocument));
        applicationDocument.updateTextContent(
                normalizeRequired(request.getTitle()),
                nextResumeText,
                nextCoverLetterText
        );

        if (hasFile(file)) {
            StoredFileMetadata storedFileMetadata = storeFile(applicationDocument.getId(), file);
            deleteStoredFileQuietly(applicationDocument.getStoredFilePath());
            applyStoredFile(applicationDocument, storedFileMetadata);
        }

        return toResponse(applicationDocument);
    }

    @Transactional
    public ApplicationDocumentResponse uploadFile(Long userId, Long documentId, MultipartFile file) {
        ApplicationDocument applicationDocument = getOwnedDocument(userId, documentId);
        if (!hasFile(file)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_DOCUMENT_FILE_REQUIRED", "Profile document file is required.");
        }

        StoredFileMetadata storedFileMetadata = storeFile(applicationDocument.getId(), file);
        deleteStoredFileQuietly(applicationDocument.getStoredFilePath());
        applyStoredFile(applicationDocument, storedFileMetadata);
        return toResponse(applicationDocument);
    }

    @Transactional
    public void delete(Long userId, Long documentId) {
        ApplicationDocument applicationDocument = getOwnedDocument(userId, documentId);
        deleteStoredFileQuietly(applicationDocument.getStoredFilePath());
        applicationDocumentRepository.delete(applicationDocument);
    }

    public ApplicationDocument getOwnedDocument(Long userId, Long documentId) {
        return applicationDocumentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_DOCUMENT_NOT_FOUND", "Profile document not found."));
    }

    public String extractStoredFileText(ApplicationDocument applicationDocument) {
        if (applicationDocument == null || !StringUtils.hasText(applicationDocument.getStoredFilePath())) {
            return null;
        }

        Path filePath = uploadRootPath.resolve(applicationDocument.getStoredFilePath()).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            log.warn("[ProfileDocuments] Stored file is not readable. documentId={}, path={}", applicationDocument.getId(), filePath);
            return null;
        }

        String originalFileName = applicationDocument.getOriginalFileName();
        if (isPdfFile(originalFileName)) {
            return extractPdfText(filePath);
        }

        if (isPlainTextFile(originalFileName)) {
            return extractPlainText(filePath);
        }

        return null;
    }

    private void validateContent(String resumeText, String coverLetterText, MultipartFile file, boolean hasExistingFile) {
        boolean hasResumeText = StringUtils.hasText(resumeText);
        boolean hasCoverLetterText = StringUtils.hasText(coverLetterText);
        boolean hasNewFile = hasFile(file);

        if (!hasResumeText && !hasCoverLetterText && !hasNewFile && !hasExistingFile) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_DOCUMENT_CONTENT_REQUIRED",
                    "At least one of resume text, cover letter text, or a file upload is required."
            );
        }
    }

    private StoredFileMetadata storeFile(Long documentId, MultipartFile file) {
        validateFile(file);

        String safeOriginalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(safeOriginalFileName);
        Path documentDirectory = uploadRootPath.resolve("document-" + documentId);
        String storedFileName = UUID.randomUUID() + extension;
        Path destination = documentDirectory.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(documentDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_DOCUMENT_FILE_UPLOAD_FAILED", "Failed to store profile document file.");
        }

        String relativePath = uploadRootPath.relativize(destination).toString().replace('\\', '/');
        return new StoredFileMetadata(
                safeOriginalFileName,
                relativePath,
                "/api/v1/profile-documents/" + documentId + "/file"
        );
    }

    private void validateFile(MultipartFile file) {
        if (!hasFile(file)) {
            return;
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_DOCUMENT_FILE_TOO_LARGE", "Profile document file must be 10MB or smaller.");
        }
    }

    private void applyStoredFile(ApplicationDocument applicationDocument, StoredFileMetadata storedFileMetadata) {
        applicationDocument.updateFileMetadata(
                storedFileMetadata.originalFileName(),
                storedFileMetadata.storedFilePath(),
                storedFileMetadata.fileUrl()
        );
    }

    private boolean isPdfFile(String fileName) {
        return StringUtils.hasText(fileName) && fileName.toLowerCase().endsWith(".pdf");
    }

    private boolean isPlainTextFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String normalized = fileName.toLowerCase();
        return normalized.endsWith(".txt") || normalized.endsWith(".md");
    }

    private String extractPdfText(Path filePath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(filePath))) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            return normalizeExtractedText(text);
        } catch (IOException exception) {
            log.warn("[ProfileDocuments] Failed to extract PDF text. path={}", filePath, exception);
            return null;
        }
    }

    private String extractPlainText(Path filePath) {
        try {
            return normalizeExtractedText(Files.readString(filePath));
        } catch (IOException exception) {
            log.warn("[ProfileDocuments] Failed to read plain text document. path={}", filePath, exception);
            return null;
        }
    }

    private String normalizeExtractedText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value
                .replace("\r\n", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("(?m)^\\s+$", "")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_EXTRACTED_TEXT_LENGTH) {
            return normalized.substring(0, MAX_EXTRACTED_TEXT_LENGTH).trim() + "\n[truncated]";
        }
        return normalized;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private boolean hasStoredFile(ApplicationDocument applicationDocument) {
        return StringUtils.hasText(applicationDocument.getStoredFilePath());
    }

    private void deleteStoredFileQuietly(String storedFilePath) {
        if (!StringUtils.hasText(storedFilePath)) {
            return;
        }

        try {
            Path path = uploadRootPath.resolve(storedFilePath).normalize();
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The database state should remain the source of truth even if file cleanup fails.
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private String sanitizeFileName(String originalFileName) {
        String candidate = StringUtils.hasText(originalFileName) ? originalFileName : "profile-document";
        return Path.of(candidate).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index) : "";
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ApplicationDocumentResponse toResponse(ApplicationDocument applicationDocument) {
        return new ApplicationDocumentResponse(
                applicationDocument.getId(),
                applicationDocument.getUser().getId(),
                applicationDocument.getTitle(),
                applicationDocument.getResumeText(),
                applicationDocument.getCoverLetterText(),
                applicationDocument.getOriginalFileName(),
                applicationDocument.getStoredFilePath(),
                applicationDocument.getFileUrl(),
                applicationDocument.getCreatedAt(),
                applicationDocument.getUpdatedAt()
        );
    }

    public record ApplicationDocumentFileDownload(
            Resource resource,
            String originalFileName,
            String contentType
    ) {
    }

    private record StoredFileMetadata(
            String originalFileName,
            String storedFilePath,
            String fileUrl
    ) {
    }
}
