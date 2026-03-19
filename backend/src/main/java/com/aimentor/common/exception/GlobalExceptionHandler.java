package com.aimentor.common.exception;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.domain.voice.exception.VoiceException;
import com.aimentor.external.ai.AiServiceException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts server-side exceptions into the standard API error response format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.error("[GlobalException] ApiException occurred code={}, status={}, message={}", ex.getErrorCode(), ex.getHttpStatus(), ex.getMessage(), ex);
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(VoiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoiceException(VoiceException ex) {
        log.error(
                "[GlobalException] VoiceException occurred code={}, status={}, message={}, details={}",
                ex.getErrorCode(),
                ex.getHttpStatus(),
                ex.getMessage(),
                ex.getDetails(),
                ex
        );
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("[GlobalException] MethodArgumentNotValidException occurred message={}", message, ex);
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("[GlobalException] BindException occurred message={}", message, ex);
        return ResponseEntity.badRequest().body(ApiResponse.error("BIND_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("[GlobalException] HttpMessageNotReadableException occurred", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST_BODY", "Request body is missing or malformed."));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceException(AiServiceException ex) {
        log.error("[GlobalException] AiServiceException occurred", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("AI_SERVICE_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.error("[GlobalException] AccessDeniedException occurred", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", "Access is denied."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("[GlobalException] Exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected server error occurred."));
    }
}
