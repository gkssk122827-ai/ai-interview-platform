package com.aimentor.domain.profile.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.profile.dto.request.ApplicationDocumentUpsertRequest;
import com.aimentor.domain.profile.dto.response.ApplicationDocumentResponse;
import com.aimentor.domain.profile.service.ApplicationDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/profile-documents", "/api/profile-documents", "/api/v1/profiles/documents"})
public class ApplicationDocumentController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationDocumentController.class);

    private final ApplicationDocumentService applicationDocumentService;

    public ApplicationDocumentController(ApplicationDocumentService applicationDocumentService) {
        this.applicationDocumentService = applicationDocumentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ApplicationDocumentResponse> createJson(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ApplicationDocumentUpsertRequest request
    ) {
        return ApiResponse.success(applicationDocumentService.create(authenticatedUser.userId(), request, null));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplicationDocumentResponse> createMultipart(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ModelAttribute ApplicationDocumentUpsertRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.success(applicationDocumentService.create(authenticatedUser.userId(), request, file));
    }

    @GetMapping
    public ApiResponse<List<ApplicationDocumentResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String keyword
    ) {
        log.info("[ProfileDocuments] Controller entered: list keyword={}", keyword);
        if (authenticatedUser != null) {
            log.info(
                    "[ProfileDocuments] Auth user: id={}, email={}, role={}",
                    authenticatedUser.userId(),
                    authenticatedUser.email(),
                    authenticatedUser.role()
            );
        } else {
            log.info("[ProfileDocuments] Auth user: anonymous");
        }

        log.info("[ProfileDocuments] Service call start: list");
        List<ApplicationDocumentResponse> response = applicationDocumentService.list(
                authenticatedUser == null ? null : authenticatedUser.userId(),
                keyword
        );
        log.info("[ProfileDocuments] Service call complete: list count={}", response.size());
        return ApiResponse.success(response);
    }

    @GetMapping("/{documentId}")
    public ApiResponse<ApplicationDocumentResponse> get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId
    ) {
        return ApiResponse.success(applicationDocumentService.get(authenticatedUser.userId(), documentId));
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId
    ) {
        ApplicationDocumentService.ApplicationDocumentFileDownload fileDownload = applicationDocumentService.loadFile(authenticatedUser.userId(), documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDownload.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileDownload.originalFileName()).build().toString()
                )
                .body(fileDownload.resource());
    }

    @PutMapping(value = "/{documentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ApplicationDocumentResponse> updateJson(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId,
            @Valid @RequestBody ApplicationDocumentUpsertRequest request
    ) {
        return ApiResponse.success(applicationDocumentService.update(authenticatedUser.userId(), documentId, request, null));
    }

    @PutMapping(value = "/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplicationDocumentResponse> updateMultipart(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId,
            @Valid @ModelAttribute ApplicationDocumentUpsertRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.success(applicationDocumentService.update(authenticatedUser.userId(), documentId, request, file));
    }

    @PostMapping(value = "/{documentId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplicationDocumentResponse> uploadFile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(applicationDocumentService.uploadFile(authenticatedUser.userId(), documentId, file));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long documentId
    ) {
        applicationDocumentService.delete(authenticatedUser.userId(), documentId);
        return ApiResponse.success();
    }
}
