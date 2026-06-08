package com.federation.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.federation.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a structured JSON 401 when an unauthenticated request hits a protected endpoint.
 * Replaces Spring Security's default HTML redirect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest  request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        log.warn("Unauthorized [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Authentication required. Please provide a valid Bearer token.",
                        request.getRequestURI())
        );
    }
}
