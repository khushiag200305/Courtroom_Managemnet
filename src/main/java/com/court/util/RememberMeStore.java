package com.court.util;

import com.court.model.Role;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * "Remember my username" on the login screen, implemented with
 * <b>object serialization</b>: a {@link RememberedLogin} object is written to
 * {@code ~/.courtroom/remember.ser} and read back on the next start.
 * Only the username and role are stored - never the password.
 */
public final class RememberMeStore {

    private static final Logger LOG = Logger.getLogger(RememberMeStore.class.getName());
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".courtroom", "remember.ser");

    /** The object that gets serialized. Records can be Serializable too. */
    public record RememberedLogin(String username, Role role, LocalDateTime savedAt) implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;   // changed when the role field was added
    }

    private RememberMeStore() {
    }

    public static void save(String username, Role role) {
        try {
            Files.createDirectories(FILE.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE.toFile()))) {
                out.writeObject(new RememberedLogin(username, role, LocalDateTime.now()));
            }
        } catch (IOException e) {
            LOG.warning("Could not save remembered login: " + e.getMessage());
        }
    }

    public static Optional<RememberedLogin> load() {
        if (!Files.exists(FILE)) {
            return Optional.empty();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE.toFile()))) {
            return Optional.of((RememberedLogin) in.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // e.g. InvalidClassException when the file was written by an older version
            LOG.warning("Ignoring unreadable remember-me file: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void clear() {
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException e) {
            LOG.warning("Could not delete remembered login: " + e.getMessage());
        }
    }
}
