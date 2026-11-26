package com.court.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Shortcut methods for JavaFX dialogs (user feedback).
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void warning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    /** Asks a yes/no question. Returns true only if the user clicks OK. */
    public static boolean confirm(String title, String message) {
        Alert alert = build(Alert.AlertType.CONFIRMATION, title, message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String message) {
        build(type, title, message).showAndWait();
    }

    private static Alert build(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (SceneManager.getStage() != null && SceneManager.getStage().isShowing()) {
            alert.initOwner(SceneManager.getStage());
        }
        alert.getDialogPane().getStylesheets().add(
                AlertUtil.class.getResource("/css/style.css").toExternalForm());
        return alert;
    }
}
