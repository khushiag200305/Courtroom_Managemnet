package com.court.exception;

import com.court.model.CaseStatus;

import java.io.Serial;

/**
 * Thrown when an action would move a case to a status that is not allowed,
 * e.g. trying to register a case that is already disposed.
 */
public class InvalidStatusTransitionException extends ValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidStatusTransitionException(CaseStatus from, CaseStatus to) {
        super("A case that is " + from.getDisplayName().toLowerCase()
                + " cannot be moved to " + to.getDisplayName().toLowerCase() + ".");
    }
}
