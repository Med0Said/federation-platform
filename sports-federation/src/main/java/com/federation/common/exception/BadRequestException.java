package com.federation.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends FederationException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
