package com.bankpro.exception;

/** 400 tied to one input field (e.g. an invalid phone number), so the form can highlight that field. */
public class InvalidFieldException extends BadRequestException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
