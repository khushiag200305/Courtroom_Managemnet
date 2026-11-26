package com.court.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Reads application settings from .properties files.
 * <ul>
 *     <li>{@code app.properties} - court name, hearing slots, folders</li>
 *     <li>{@code db.properties}  - database URL, user, password</li>
 *     <li>{@code db.local.properties} (optional, in the folder the app runs from) -
 *         overrides db.properties so your real password stays out of Git</li>
 * </ul>
 * All members are static: there is only one configuration for the whole app.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());
    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    private AppConfig() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loadFromClasspath("/app.properties");
        loadFromClasspath("/db.properties");

        Path local = Path.of("db.local.properties");
        if (Files.exists(local)) {
            try (Reader reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                PROPS.load(reader);
                LOG.info("Loaded local overrides from " + local.toAbsolutePath());
            } catch (IOException e) {
                LOG.warning("Could not read " + local + ": " + e.getMessage());
            }
        }
        loaded = true;
    }

    private static void loadFromClasspath(String resource) {
        try (InputStream in = AppConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                LOG.warning("Config file not found on classpath: " + resource);
                return;
            }
            PROPS.load(in);
        } catch (IOException e) {
            LOG.warning("Could not read " + resource + ": " + e.getMessage());
        }
    }

    public static String get(String key) {
        load();
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        load();
        return PROPS.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Invalid number for " + key + ": " + value);
            return defaultValue;
        }
    }

    public static LocalTime getTime(String key, LocalTime defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            LOG.warning("Invalid time for " + key + ": " + value);
            return defaultValue;
        }
    }

    /** Used by unit tests to supply settings without files. */
    static void set(String key, String value) {
        PROPS.setProperty(key, value);
        loaded = true;
    }
}
