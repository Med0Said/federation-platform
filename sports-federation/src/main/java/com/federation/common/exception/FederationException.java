package com.federation.common.exception;

import org.springframework.http.HttpStatus;

// ============================================================
// Base exception
// ============================================================

/**
 * Base class for all application-specific exceptions.
 */
public abstract class FederationException extends RuntimeException {

    private final HttpStatus status;

    protected FederationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected FederationException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}



