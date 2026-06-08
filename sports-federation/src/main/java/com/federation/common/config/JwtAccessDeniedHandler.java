package com.federation.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.federation.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a structured JSON 403 when an authenticated user lacks the required authority.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest  request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        log.warn("Access denied [{}] {} for principal: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error(
                        HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to access this resource.",
                        request.getRequestURI())
        );
    }
}
