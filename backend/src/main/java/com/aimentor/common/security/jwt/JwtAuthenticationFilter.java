package com.aimentor.common.security.jwt;

import com.aimentor.common.exception.ApiException;
import com.aimentor.common.security.AuthenticatedUser;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/auth/",
            "/api/v1/auth/",
            "/api/v1/health"
    );

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            try {
                String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
                JwtTokenClaims claims = jwtTokenProvider.parse(accessToken);

                if (claims.tokenType() == JwtTokenType.ACCESS) {
                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                            claims.userId(),
                            claims.email(),
                            claims.role()
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ApiException exception) {
                log.warn(
                        "[JwtAuthentication] Invalid token requestUri={}, method={}, code={}, message={}",
                        request.getRequestURI(),
                        request.getMethod(),
                        exception.getErrorCode(),
                        exception.getMessage()
                );
                SecurityContextHolder.clearContext();
                if (shouldIgnoreInvalidToken(request)) {
                    log.info("[JwtAuthentication] Ignoring invalid token for public path requestUri={}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                writeUnauthorizedResponse(response, exception.getErrorCode(), exception.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldIgnoreInvalidToken(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"data\":null,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}"
        );
    }
}
