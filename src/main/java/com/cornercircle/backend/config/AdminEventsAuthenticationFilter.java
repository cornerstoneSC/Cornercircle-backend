package com.cornercircle.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminEventsAuthenticationFilter extends OncePerRequestFilter {
    private final String adminToken;

    public AdminEventsAuthenticationFilter(@Value("${MEMBERSHIP_ADMIN_TOKEN:}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/admin/events");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (adminToken.length() < 32) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Events administration is not configured.");
            return;
        }
        byte[] expected = ("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8);
        String authorization = request.getHeader("Authorization");
        byte[] supplied = authorization == null ? new byte[0] : authorization.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid admin token.");
            return;
        }
        chain.doFilter(request, response);
    }
}
