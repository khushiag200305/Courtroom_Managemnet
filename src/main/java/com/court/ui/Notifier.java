package com.court.ui;

import com.court.util.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A small "toast" notification at the bottom of the window that fades away by itself,
 * for success messages that do not need a dialog ("Judge added", "Cause list exported").
 */
public final class Notifier {

    private Notifier() {
    }

    public static void success(String message) {
        show(message, "fth-check-circle", "toast-success");
    }

    public static void info(String message) {
        show(message, "fth-info", "toast-info");
    }

    private static void show(String message, String icon, String style) {
        Stage stage = SceneManager.getStage();
        if (stage == null || stage.getScene() == null) return;

        Label label = new Label(message, new FontIcon(icon));
        label.getStyleClass().addAll("toast", style);
        StackPane box = new StackPane(label);
        box.getStylesheets().add(SceneManager.css());

        Popup popup = new Popup();
        popup.getContent().add(box);
        popup.setAutoFix(true);
        popup.show(stage);
        // centre it near the bottom of the window once its size is known
        popup.setX(stage.getX() + (stage.getWidth() - box.getWidth()) / 2);
        popup.setY(stage.getY() + stage.getHeight() - box.getHeight() - 48);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), box);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition wait = new PauseTransition(Duration.seconds(2.6));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), box);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        SequentialTransition all = new SequentialTransition(fadeIn, wait, fadeOut);
        all.setOnFinished(e -> popup.hide());
        all.play();
    }
}
