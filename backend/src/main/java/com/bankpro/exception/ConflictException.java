package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Request conflicts with current state, e.g. duplicate email (409). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
