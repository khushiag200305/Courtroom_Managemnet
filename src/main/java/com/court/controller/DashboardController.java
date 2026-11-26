package com.court.controller;

import com.court.dao.DashboardDao;
import com.court.dao.impl.DashboardDaoImpl;
import com.court.model.User;
import com.court.service.AuthService;
import com.court.util.AlertUtil;
import com.court.util.AppConfig;
import com.court.util.CourtClock;
import com.court.util.SceneManager;
import com.court.util.SessionManager;
import com.court.util.UiFactory;
import javafx.event.ActionEvent;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviour for all three dashboards: sidebar, user card, logout and switching
 * the centre content.
 * <p>
 * Abstract class + inheritance: RegistrarDashboardController, JudgeDashboardController and
 * LawyerDashboardController extend this class and only add what is different for their role.
 */
public abstract class DashboardController {

    protected static final Logger LOG = Logger.getLogger(DashboardController.class.getName());
    protected static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    protected static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d MMM");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");

    @FXML protected VBox sidebar;
    @FXML protected Button homeButton;
    @FXML protected Label courtNameLabel;
    @FXML protected Label avatarLabel;
    @FXML protected Label userNameLabel;
    @FXML protected Label roleLabel;
    @FXML protected Label welcomeLabel;
    @FXML protected Label dateLabel;
    @FXML protected Label sessionLabel;
    @FXML protected StackPane contentArea;

    /** Shared by all dashboards for their read-only figures. */
    protected final DashboardDao dashboardDao = new DashboardDaoImpl();

    private Node homeView;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        courtNameLabel.setText(AppConfig.get("court.name", "District Court"));
        avatarLabel.setText(UiFactory.initials(user.getName()));
        userNameLabel.setText(user.getName());
        roleLabel.setText(user.getRole().getDisplayName());
        welcomeLabel.setText(greeting() + ", " + firstName(user.getName()));
        dateLabel.setText(LocalDate.now().format(LONG_DATE));

        // "Court in session" badge with a green or grey dot
        LocalDateTime now = LocalDateTime.now();
        boolean sitting = CourtClock.isInSession(now);
        sessionLabel.setText(CourtClock.status(now));
        sessionLabel.setGraphic(UiFactory.dot(sitting ? "dot-green" : "dot"));
        sessionLabel.getStyleClass().add(sitting ? "session-live" : "session-closed");

        if (!contentArea.getChildren().isEmpty()) {
            homeView = contentArea.getChildren().get(0);
        }
        setActive(homeButton);
        onDashboardReady(user);
    }

    /** Each dashboard loads its own home-screen data here (also called when "Dashboard" is clicked again). */
    protected abstract void onDashboardReady(User user);

    @FXML
    public void showHome() {
        setActive(homeButton);
        contentArea.getChildren().setAll(homeView);
        onDashboardReady(SessionManager.getCurrentUser());
    }

    /**
     * Default handler for sidebar buttons. Screens that are not built yet show a placeholder;
     * later steps replace this by calling {@link #loadContent(String, Button)}.
     */
    @FXML
    public void handleNav(ActionEvent event) {
        Button button = (Button) event.getSource();
        contentArea.getChildren().setAll(UiFactory.comingSoon(button.getText()));
        setActive(button);
    }

    /** Loads an FXML screen into the centre of the dashboard and returns its controller. */
    protected <T> T loadContent(String fxml, Button navButton) {
        try {
            FXMLLoader loader = SceneManager.loader(fxml);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            setActive(navButton);
            return loader.getController();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load " + fxml, e);
            AlertUtil.error("Error", "Couldn't open this screen.");
            return null;
        }
    }

    /** Shows a screen that was built in code (not FXML) in the centre of the dashboard. */
    protected void showContent(Node view, Button navButton) {
        contentArea.getChildren().setAll(view);
        setActive(navButton);
    }

    /** Highlights the selected sidebar button (lookupAll also finds buttons wrapped in a StackPane). */
    protected void setActive(Button selected) {
        sidebar.lookupAll(".nav-button").forEach(node -> node.getStyleClass().remove("active"));
        if (selected != null && !selected.getStyleClass().contains("active")) {
            selected.getStyleClass().add("active");
        }
    }

    @FXML
    public void handleLogout() {
        if (AlertUtil.confirm("Log out", "Do you want to log out?")) {
            User user = SessionManager.getCurrentUser();
            new AuthService().logout(user);
            SessionManager.logout();
            SceneManager.switchTo("login.fxml", "Login");
        }
    }

    /** "Justice Anil Mehta" -> "Anil Mehta", for tables whose column is already headed "Judge". */
    protected static String withoutTitle(String judgeName) {
        return judgeName == null ? "-" : judgeName.replaceFirst("^Justice\\s+", "");
    }

    /** Shows an error dialog for a failed background load. */
    protected void showLoadError(Throwable error) {
        AlertUtil.error("Couldn't load data", error.getMessage());
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    /** "Justice Anil Mehta" -> "Justice Mehta", "Adv. Neha Verma" -> "Neha", "Rajesh Khanna" -> "Rajesh". */
    private String firstName(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts[0].equalsIgnoreCase("Justice") && parts.length > 1) {
            return "Justice " + parts[parts.length - 1];
        }
        if (parts[0].endsWith(".") && parts.length > 1) {
            return parts[1];
        }
        return parts[0];
    }
}
