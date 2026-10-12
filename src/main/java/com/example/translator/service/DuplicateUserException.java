package com.example.translator.service;

/** Thrown when the email or VIT ID is already registered. */
public class DuplicateUserException extends RuntimeException {

    // Name of the form field that caused the problem ("email" or "vitId")
    private final String field;

    public DuplicateUserException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
