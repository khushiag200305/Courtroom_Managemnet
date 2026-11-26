package com.court.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Sets up java.util.logging using {@code logging.properties}.
 * Every class then simply does {@code Logger.getLogger(MyClass.class.getName())}
 * and its messages go to the console and to {@code logs/court.log}.
 */
public final class AppLogger {

    private AppLogger() {
    }

    public static void init() {
        try {
            // FileHandler cannot create folders, so make sure logs/ exists first
            Files.createDirectories(Path.of("logs"));
            try (InputStream in = AppLogger.class.getResourceAsStream("/logging.properties")) {
                if (in != null) {
                    LogManager.getLogManager().readConfiguration(in);
                }
            }
        } catch (IOException e) {
            System.err.println("Logging setup failed, using defaults: " + e.getMessage());
        }
        Logger.getLogger(AppLogger.class.getName()).info("===== Courtroom Management System started =====");
    }

    /** Flushes and closes log files - called from Application.stop(). */
    public static void shutdown() {
        Logger.getLogger(AppLogger.class.getName()).info("===== Application stopped =====");
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            handler.flush();
            handler.close();
        }
    }
}
