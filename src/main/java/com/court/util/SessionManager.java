package com.court.util;

import com.court.model.User;

/**
 * Remembers who is logged in while the app is running.
 * <p>
 * Static members are shared by the whole application, so any controller can call
 * {@code SessionManager.getCurrentUser()} without passing the user around.
 */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
