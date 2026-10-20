package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Request is well-formed but violates a business rule (400). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
