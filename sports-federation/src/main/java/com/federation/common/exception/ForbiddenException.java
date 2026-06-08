package com.federation.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends FederationException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
