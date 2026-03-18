package com.aimentor.common.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wraps all API responses in a consistent success/data/error envelope.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorBody error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    /**
     * Carries machine-readable error information for failed requests.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ErrorBody {
        private final String code;
        private final String message;
    }
}
