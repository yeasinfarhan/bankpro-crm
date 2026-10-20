package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Authenticated but not allowed to perform this action (403). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
