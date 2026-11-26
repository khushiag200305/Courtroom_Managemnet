package com.court.model;

/**
 * The three kinds of users who can log in to the system.
 */
public enum Role {
    REGISTRAR("Registrar"),
    JUDGE("Judge"),
    LAWYER("Lawyer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
