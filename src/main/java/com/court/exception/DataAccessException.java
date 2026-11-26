package com.court.exception;

import java.io.Serial;

/**
 * Wraps low-level {@link java.sql.SQLException}s thrown inside the DAO layer.
 * <p>
 * It is <b>unchecked</b> (extends {@link RuntimeException}) so that services and
 * controllers do not need JDBC-specific code. The original SQLException is kept
 * as the cause for logging and debugging.
 */
public class DataAccessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
