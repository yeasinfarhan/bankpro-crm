package com.bankpro.exception;

/** 409 tied to one input field (e.g. duplicate phone), so the form can highlight that field. */
public class FieldConflictException extends ConflictException {

    private final String field;

    public FieldConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
