package com.federation.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Structured validation error for 400 responses.
 */
@Getter
@AllArgsConstructor
public class ValidationErrorResponse {

    private final int               status;
    private final String            message;
    private final List<FieldError>  errors;
    private final String            path;

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }
}
