package com.qapilot.server.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer access token을 검증해 SecurityContext에 사용자 정보를 설정한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } catch (QapilotException e) {
            writeError(response, e.errorCode(), e.getMessage());
        }
    }

    private void authenticate(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return;
        }
        JwtClaims claims = tokenProvider.verify(token);
        if (!"access".equals(claims.type())) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
        AuthenticatedUser principal = new AuthenticatedUser(claims.subject(), claims.email(), claims.role());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 1순위: Authorization: Bearer 헤더.
     * 2순위: SSE 엔드포인트 (/stream) 전용 ?access_token= 쿼리 파라미터 — 브라우저 EventSource 가
     *        헤더를 못 보내므로 query param 으로 폴백. 쿼리 토큰은 액세스 로그에 남으므로 SSE 외엔 허용 안 함.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        if (request.getRequestURI().endsWith("/stream")) {
            String queryToken = request.getParameter("access_token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }
        return null;
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code.code(), message));
    }
}
