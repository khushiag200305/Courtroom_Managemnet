package com.court.controller;

import com.court.exception.AuthenticationException;
import com.court.model.User;
import com.court.service.AuthService;
import com.court.util.AppConfig;
import com.court.util.BackgroundTask;
import com.court.util.RememberMeStore;
import com.court.util.SceneManager;
import com.court.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Controller for login.fxml.
 */
public class LoginController {

    private static final String ERROR_STYLE = "field-error";

    @FXML private Label courtNameLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final AuthService authService = new AuthService();

    /** Called automatically by FXMLLoader after the @FXML fields are injected. */
    @FXML
    private void initialize() {
        courtNameLabel.setText(AppConfig.get("court.name", "District Court"));

        // Deserialize the remembered username, if any
        RememberMeStore.load().ifPresent(saved -> {
            usernameField.setText(saved.username());
            rememberMeCheck.setSelected(true);
            Platform.runLater(passwordField::requestFocus);
        });

        // Listeners: remove the red error highlight as soon as the user types again
        usernameField.textProperty().addListener((obs, oldText, newText) -> clearError(usernameField));
        passwordField.textProperty().addListener((obs, oldText, newText) -> clearError(passwordField));
    }

    /** Event handler for the Login button and the Enter key in the password field. */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!validate(username, password)) {
            return;
        }

        setBusy(true);
        BackgroundTask.run(
                () -> authService.login(username, password),
                this::onLoginSuccess,
                this::onLoginFailed);
    }

    private boolean validate(String username, String password) {
        boolean ok = true;
        if (username.isEmpty()) {
            markError(usernameField);
            ok = false;
        }
        if (password.isEmpty()) {
            markError(passwordField);
            ok = false;
        }
        if (!ok) {
            showError("Please enter both username and password.");
        }
        return ok;
    }

    private void onLoginSuccess(User user) {
        setBusy(false);
        SessionManager.login(user);

        if (rememberMeCheck.isSelected()) {
            RememberMeStore.save(user.getUsername());
        } else {
            RememberMeStore.clear();
        }

        // Polymorphism: each User subclass knows which dashboard to open
        SceneManager.switchTo(user.getDashboardFxml(), user.getDashboardTitle());
    }

    private void onLoginFailed(Throwable error) {
        setBusy(false);
        passwordField.clear();
        if (error instanceof AuthenticationException) {
            showError(error.getMessage());
            markError(passwordField);
        } else {
            showError("Could not reach the database. Please check that MySQL is running.");
        }
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Signing in..." : "Login");
        loadingIndicator.setVisible(busy);
        if (busy) {
            hideError();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void markError(Control field) {
        if (!field.getStyleClass().contains(ERROR_STYLE)) {
            field.getStyleClass().add(ERROR_STYLE);
        }
    }

    private void clearError(Control field) {
        field.getStyleClass().remove(ERROR_STYLE);
    }
}
