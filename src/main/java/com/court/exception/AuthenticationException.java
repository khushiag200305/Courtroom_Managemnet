package com.court.exception;

import java.io.Serial;

/**
 * Thrown when login fails (wrong username/password or inactive account).
 * <p>
 * This is a <b>checked</b> exception (extends {@link Exception}) because the caller
 * is expected to handle it - the login screen shows the message to the user.
 */
public class AuthenticationException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
