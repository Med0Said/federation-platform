package com.federation.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends FederationException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
