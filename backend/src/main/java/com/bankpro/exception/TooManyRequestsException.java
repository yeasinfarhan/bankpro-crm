package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Rate limit exceeded (429). */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
