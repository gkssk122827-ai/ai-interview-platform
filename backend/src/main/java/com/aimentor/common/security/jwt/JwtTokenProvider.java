package com.aimentor.common.security.jwt;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String email, Role role) {
        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.accessTokenExpirationSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("tokenType", JwtTokenType.ACCESS.name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String email, Role role) {
        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("tokenType", JwtTokenType.REFRESH.name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtTokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new JwtTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    JwtTokenType.valueOf(claims.get("tokenType", String.class)),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is invalid or expired.");
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.refreshTokenExpirationSeconds();
    }
}
