package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Login failed. Always uses the same generic message (401). */
public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
