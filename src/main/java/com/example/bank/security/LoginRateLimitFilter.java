package com.example.bank.security;

import com.example.bank.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final LoginRateLimiter limiter;
    private final ObjectMapper mapper;
    private final SecurityAuditService audit;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getContextPath() + "/api/auth/login").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Do not trust client-controlled X-Forwarded-For headers.
        var decision = limiter.acquire(request.getRemoteAddr());
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        if (decision.audit()) audit.recordFailure(SecurityAuditEvent.Type.LOGIN_THROTTLED, null);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), 429,
                "LOGIN_RATE_LIMITED", "Too many login attempts; try again later",
                request.getRequestURI(), UUID.randomUUID()));
    }
}
