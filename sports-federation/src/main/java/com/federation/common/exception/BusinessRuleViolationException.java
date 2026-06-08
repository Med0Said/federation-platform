package com.federation.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends FederationException {
    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
