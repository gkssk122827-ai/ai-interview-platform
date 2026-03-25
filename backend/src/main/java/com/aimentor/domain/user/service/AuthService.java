package com.aimentor.domain.user.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.common.security.jwt.JwtTokenClaims;
import com.aimentor.common.security.jwt.JwtTokenProvider;
import com.aimentor.common.security.jwt.JwtTokenType;
import com.aimentor.domain.user.dto.request.LoginRequest;
import com.aimentor.domain.user.dto.request.LogoutRequest;
import com.aimentor.domain.user.dto.request.MyInfoUpdateRequest;
import com.aimentor.domain.user.dto.request.RefreshTokenRequest;
import com.aimentor.domain.user.dto.request.SignupRequest;
import com.aimentor.domain.user.dto.response.AuthTokenResponse;
import com.aimentor.domain.user.dto.response.MyInfoResponse;
import com.aimentor.domain.user.entity.Role;
import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.entity.UserStatus;
import com.aimentor.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements registration, login, token rotation, logout, and current-user lookup.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthTokenResponse register(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered.");
        }

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
        ensureUserAccessible(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        JwtTokenClaims claims = jwtTokenProvider.parse(request.refreshToken());

        if (claims.tokenType() != JwtTokenType.REFRESH) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", "Refresh token is required.");
        }

        User user = getUser(claims.userId());
        ensureUserAccessible(user);

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(request.refreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "Refresh token does not match the stored token.");
        }

        if (user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token has expired.");
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        JwtTokenClaims claims = jwtTokenProvider.parse(request.refreshToken());

        if (claims.tokenType() != JwtTokenType.REFRESH) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", "Refresh token is required.");
        }

        if (!claims.userId().equals(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_USER_MISMATCH", "Authenticated user does not match the refresh token.");
        }

        User user = getUser(userId);
        if (user.getRefreshToken() == null || !request.refreshToken().equals(user.getRefreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "Refresh token does not match the stored token.");
        }

        user.clearRefreshToken();
    }

    public MyInfoResponse getMyInfo(Long userId) {
        User user = getUser(userId);
        return new MyInfoResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public MyInfoResponse updateMyInfo(Long userId, MyInfoUpdateRequest request) {
        User user = getUser(userId);
        ensureUserAccessible(user);
        user.updateProfile(request.name().trim(), request.phone().trim());
        return getMyInfo(userId);
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole());

        LocalDateTime accessTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds());
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());

        user.updateRefreshToken(refreshToken, refreshTokenExpiresAt);

        return new AuthTokenResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                accessTokenExpiresAt,
                refreshToken,
                refreshTokenExpiresAt
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
    }

    private void ensureUserAccessible(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "정지된 회원은 로그인할 수 없습니다.");
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_WITHDRAWN", "탈퇴 처리된 회원입니다.");
        }
    }
}
