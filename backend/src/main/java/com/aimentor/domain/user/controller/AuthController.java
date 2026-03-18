package com.aimentor.domain.user.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.user.dto.request.LoginRequest;
import com.aimentor.domain.user.dto.request.LogoutRequest;
import com.aimentor.domain.user.dto.request.RefreshTokenRequest;
import com.aimentor.domain.user.dto.request.SignupRequest;
import com.aimentor.domain.user.dto.response.AuthTokenResponse;
import com.aimentor.domain.user.dto.response.MyInfoResponse;
import com.aimentor.domain.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles JWT-based authentication and current-user endpoints.
 */
@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/register", "/signup"})
    public ApiResponse<AuthTokenResponse> register(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(authenticatedUser.userId(), request);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(authService.getMyInfo(authenticatedUser.userId()));
    }
}
