package com.court.controller;

import com.court.model.User;
import com.court.service.AuthService;
import com.court.util.AlertUtil;
import com.court.util.AppConfig;
import com.court.util.SceneManager;
import com.court.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviour for all three dashboards (header, sidebar, logout, content switching).
 * <p>
 * Abstract class + inheritance: RegistrarDashboardController, JudgeDashboardController and
 * LawyerDashboardController extend this class and only add what is different for their role.
 */
public abstract class DashboardController {

    protected static final Logger LOG = Logger.getLogger(DashboardController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");

    @FXML protected Label courtNameLabel;
    @FXML protected Label userNameLabel;
    @FXML protected Label roleLabel;
    @FXML protected Label welcomeLabel;
    @FXML protected Label dateLabel;
    @FXML protected StackPane contentArea;
    @FXML protected VBox sidebar;
    @FXML protected Button homeButton;

    private Node homeView;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        courtNameLabel.setText(AppConfig.get("court.name", "District Court"));
        userNameLabel.setText(user.getName());
        roleLabel.setText(user.getRole().getDisplayName());
        welcomeLabel.setText(greeting() + ", " + user.getName());
        dateLabel.setText(LocalDate.now().format(DATE_FORMAT));

        if (!contentArea.getChildren().isEmpty()) {
            homeView = contentArea.getChildren().get(0);
        }
        setActive(homeButton);
        onDashboardReady(user);
    }

    /** Each dashboard loads its own home-screen data here. */
    protected abstract void onDashboardReady(User user);

    @FXML
    public void showHome() {
        setActive(homeButton);
        contentArea.getChildren().setAll(homeView);
        onDashboardReady(SessionManager.getCurrentUser());
    }

    /**
     * Loads an FXML screen into the centre of the dashboard and returns its controller.
     */
    protected <T> T loadContent(String fxml, Button navButton) {
        try {
            FXMLLoader loader = SceneManager.loader(fxml);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            setActive(navButton);
            return loader.getController();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load " + fxml, e);
            AlertUtil.error("Error", "Could not open this screen.");
            return null;
        }
    }

    /** Highlights the selected sidebar button. */
    protected void setActive(Button selected) {
        if (sidebar == null) return;
        sidebar.getChildren().forEach(node -> node.getStyleClass().remove("active"));
        if (selected != null) {
            selected.getStyleClass().add("active");
        }
    }

    @FXML
    public void handleLogout() {
        if (AlertUtil.confirm("Logout", "Are you sure you want to log out?")) {
            User user = SessionManager.getCurrentUser();
            new AuthService().logout(user);
            SessionManager.logout();
            SceneManager.switchTo("login.fxml", "Login");
        }
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }
}
