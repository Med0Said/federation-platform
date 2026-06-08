package com.federation.common.exception;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ----------------------------------------------------------------
    // 400 – Validation (Bean Validation / @Valid)
    // ----------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ValidationErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ValidationErrorResponse.FieldError(
                        fe.getField(),
                        fe.getRejectedValue(),
                        fe.getDefaultMessage()))
                .toList();

        ValidationErrorResponse body = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed. Check the 'errors' field for details.",
                fieldErrors,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ----------------------------------------------------------------
    // 400 – Constraint violations (path / query params)
    // ----------------------------------------------------------------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ValidationErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> new ValidationErrorResponse.FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getInvalidValue(),
                        cv.getMessage()))
                .toList();

        ValidationErrorResponse body = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation.",
                fieldErrors,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ----------------------------------------------------------------
    // 400 – Malformed JSON / type mismatch
    // ----------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request body: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String msg = String.format("Parameter '%s' should be of type '%s'.",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String msg = String.format("Required parameter '%s' is missing.", ex.getParameterName());
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    // ----------------------------------------------------------------
    // 400 – Application bad request
    // ----------------------------------------------------------------

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ----------------------------------------------------------------
    // 401 – Authentication
    // ----------------------------------------------------------------

    @ExceptionHandler({BadCredentialsException.class, UnauthorizedException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(
            DisabledException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNAUTHORIZED, "User account is disabled.", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(
            LockedException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNAUTHORIZED, "User account is locked.", request);
    }

    // ----------------------------------------------------------------
    // 403 – Access denied
    // ----------------------------------------------------------------

    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request);
    }

    // ----------------------------------------------------------------
    // 404 – Not found
    // ----------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.NOT_FOUND,
                String.format("No endpoint found for [%s %s].", ex.getHttpMethod(), ex.getRequestURL()),
                request);
    }

    // ----------------------------------------------------------------
    // 405 – Method not allowed
    // ----------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.METHOD_NOT_ALLOWED,
                String.format("HTTP method '%s' is not supported for this endpoint.", ex.getMethod()),
                request);
    }

    // ----------------------------------------------------------------
    // 409 – Conflict
    // ----------------------------------------------------------------

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Data integrity violation at [{}]: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT,
                "Data integrity violation. A record with the same unique key may already exist.", request);
    }

    // ----------------------------------------------------------------
    // 415 – Unsupported media type
    // ----------------------------------------------------------------

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                String.format("Content type '%s' is not supported.", ex.getContentType()),
                request);
    }

    // ----------------------------------------------------------------
    // 422 – Business rule violation
    // ----------------------------------------------------------------

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleViolationException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // ----------------------------------------------------------------
    // 500 – Generic / unexpected
    // ----------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message, req.getRequestURI()));
    }
}
