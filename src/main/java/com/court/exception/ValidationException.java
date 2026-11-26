package com.court.exception;

import java.io.Serial;

/**
 * Thrown when user input breaks a business rule
 * (empty required field, invalid phone number, duplicate username, ...).
 * Checked, because the screen must tell the user what to fix.
 */
public class ValidationException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
