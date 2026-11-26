package com.court.exception;

import java.io.Serial;

/**
 * Thrown when a hearing would double-book a judge, a courtroom or a lawyer,
 * or fall on a closed day. It extends {@link ValidationException}, so screens that
 * already handle validation errors also handle clashes.
 */
public class ScheduleConflictException extends ValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ScheduleConflictException(String message) {
        super(message);
    }
}
