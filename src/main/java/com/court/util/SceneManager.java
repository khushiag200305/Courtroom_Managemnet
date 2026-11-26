package com.court.util;

import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Switches screens inside the single primary {@link Stage}.
 * <p>
 * JavaFX structure: Stage (the window) -&gt; Scene (the content) -&gt; Scene Graph
 * (a tree of nodes such as BorderPane, VBox, Button). Changing screens means
 * giving the Stage a new Scene loaded from an FXML file.
 */
public final class SceneManager {

    private static final Logger LOG = Logger.getLogger(SceneManager.class.getName());
    private static final String APP_NAME = "Courtroom Management System";
    private static Stage stage;

    private SceneManager() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(960);
        stage.setMinHeight(620);
    }

    public static Stage getStage() {
        return stage;
    }

    /** Loads view/{fxml}, shows it in the main window and returns its controller. */
    public static <T> T switchTo(String fxml, String title) {
        try {
            FXMLLoader loader = loader(fxml);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(css());
            stage.setScene(scene);
            stage.setTitle(title + " | " + APP_NAME);
            stage.centerOnScreen();
            return loader.getController();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load screen " + fxml, e);
            throw new IllegalStateException("Cannot load screen " + fxml, e);
        }
    }

    /** Creates an FXMLLoader for a file in /view - used for content loaded inside a dashboard. */
    public static FXMLLoader loader(String fxml) {
        URL url = SceneManager.class.getResource("/view/" + fxml);
        if (url == null) {
            throw new IllegalArgumentException("FXML not found: /view/" + fxml);
        }
        return new FXMLLoader(url);
    }

    private static HostServices hostServices;

    /** Set once from Main.start(); used to open documents and exported files. */
    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    /** Opens a file with the computer's default program (PDF viewer, Excel, ...). */
    public static void openFile(Path file) {
        if (hostServices != null) {
            hostServices.showDocument(file.toAbsolutePath().toUri().toString());
        }
    }

    public static String css() {
        return SceneManager.class.getResource("/css/style.css").toExternalForm();
    }
}
