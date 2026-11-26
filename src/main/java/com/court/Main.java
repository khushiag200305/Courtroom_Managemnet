package com.court;

import com.court.exception.DataAccessException;
import com.court.util.AlertUtil;
import com.court.util.AppConfig;
import com.court.util.AppLogger;
import com.court.util.DBConnection;
import com.court.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Entry point of the JavaFX application.
 * <p>
 * JavaFX application lifecycle:
 * <ol>
 *     <li>{@link #init()}  - runs first, on a background thread. No UI allowed here,
 *         so we only load settings and test the database.</li>
 *     <li>{@link #start(Stage)} - runs on the JavaFX Application Thread and builds the UI.</li>
 *     <li>{@link #stop()}  - runs when the window is closed. Clean up resources here.</li>
 * </ol>
 */
public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private String startupError;   // filled in init(), shown in start()

    @Override
    public void init() {
        AppLogger.init();
        AppConfig.load();
        try {
            DBConnection.testConnection();
        } catch (DataAccessException e) {
            LOG.severe(e.getMessage());
            startupError = e.getMessage();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);

        if (startupError != null) {
            AlertUtil.error("Database connection failed", startupError);
            Platform.exit();
            return;
        }

        SceneManager.switchTo("login.fxml", "Login");
        primaryStage.show();
    }

    @Override
    public void stop() {
        AppLogger.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
