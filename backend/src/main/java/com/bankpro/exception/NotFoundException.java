package com.bankpro.exception;

import org.springframework.http.HttpStatus;

/** Record not found or not accessible (404). Also used for out-of-scope records so IDs cannot be probed. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
