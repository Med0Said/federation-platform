package com.federation.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends FederationException {
    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
