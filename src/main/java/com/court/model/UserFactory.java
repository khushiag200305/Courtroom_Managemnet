package com.court.model;

/**
 * Factory pattern: creates the right {@link User} subclass for a role.
 * Used by the DAO when turning a database row into an object.
 */
public final class UserFactory {

    private UserFactory() {
        // utility class - no objects
    }

    public static User create(Role role) {
        return switch (role) {
            case REGISTRAR -> new Registrar();
            case JUDGE -> new Judge();
            case LAWYER -> new Lawyer();
        };
    }
}
