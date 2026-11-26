package com.court.controller;

import com.court.dao.DashboardDao;
import com.court.dao.impl.DashboardDaoImpl;
import com.court.dto.CaseSummary;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.exception.AuthenticationException;
import com.court.model.Role;
import com.court.model.User;
import com.court.service.AuthService;
import com.court.util.AlertUtil;
import com.court.util.AppConfig;
import com.court.util.BackgroundTask;
import com.court.util.CourtClock;
import com.court.util.RememberMeStore;
import com.court.util.SceneManager;
import com.court.util.SessionManager;
import com.court.util.UiFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for login.fxml: role tabs, sign-in, remember-me, the court status panel
 * and the public case look-up.
 */
public class LoginController {

    private static final String ERROR_STYLE = "field-error";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    /** Sample account shown as a hint for each role (matches database/seed.sql). */
    private static final Map<Role, String> DEMO_ACCOUNTS = Map.of(
            Role.REGISTRAR, "registrar / admin123",
            Role.JUDGE, "judge1 / judge123   (judge1 to judge4)",
            Role.LAWYER, "lawyer1 / lawyer123   (lawyer1 to lawyer8)");

    @FXML private Label courtNameLabel;
    @FXML private StackPane sessionDot;
    @FXML private Label sessionLabel;
    @FXML private VBox causeListBox;
    @FXML private HBox liveStatsBox;
    @FXML private Label pendingStatLabel;
    @FXML private Label hearingsStatLabel;
    @FXML private Label disposedStatLabel;
    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton registrarTab;
    @FXML private ToggleButton judgeTab;
    @FXML private ToggleButton lawyerTab;
    @FXML private HBox usernameGroup;
    @FXML private HBox passwordGroup;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Label demoHint;

    private final AuthService authService = new AuthService();
    private final DashboardDao dashboardDao = new DashboardDaoImpl();

    /** Called automatically by FXMLLoader after the @FXML fields are injected. */
    @FXML
    private void initialize() {
        courtNameLabel.setText(AppConfig.get("court.name", "District Court"));
        setUpRoleTabs();
        showCourtSession();

        // Deserialize the remembered username and role, if any
        RememberMeStore.load().ifPresent(saved -> {
            usernameField.setText(saved.username());
            selectTab(saved.role());
            rememberMeCheck.setSelected(true);
            Platform.runLater(passwordField::requestFocus);
        });

        // Listeners: remove the red highlight as soon as the user types again
        usernameField.textProperty().addListener((obs, oldText, newText) -> clearError(usernameGroup));
        passwordField.textProperty().addListener((obs, oldText, newText) -> clearError(passwordGroup));

        loadCourtFigures();
    }

    // ------------------------------------------------------------------ role tabs

    private void setUpRoleTabs() {
        registrarTab.setUserData(Role.REGISTRAR);
        judgeTab.setUserData(Role.JUDGE);
        lawyerTab.setUserData(Role.LAWYER);

        // A ToggleGroup lets the user click the selected tab off; put it back so one is always chosen
        roleGroup.selectedToggleProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null) {
                roleGroup.selectToggle(oldTab);
            } else {
                onRoleChanged();
            }
        });
        onRoleChanged();
    }

    private Role selectedRole() {
        return (Role) roleGroup.getSelectedToggle().getUserData();
    }

    private void selectTab(Role role) {
        if (role == null) return;
        switch (role) {
            case REGISTRAR -> roleGroup.selectToggle(registrarTab);
            case JUDGE -> roleGroup.selectToggle(judgeTab);
            case LAWYER -> roleGroup.selectToggle(lawyerTab);
        }
    }

    private void onRoleChanged() {
        Role role = selectedRole();
        usernameField.setPromptText(switch (role) {
            case REGISTRAR -> "registrar";
            case JUDGE -> "judge1";
            case LAWYER -> "lawyer1";
        });
        demoHint.setText("Demo " + role.getDisplayName().toLowerCase() + " account:  " + DEMO_ACCOUNTS.get(role));
        hideError();
    }

    // ------------------------------------------------------------------ left panel

    private void showCourtSession() {
        LocalDateTime now = LocalDateTime.now();
        sessionLabel.setText(CourtClock.status(now));
        sessionDot.getStyleClass().add(CourtClock.isInSession(now) ? "session-on" : "session-off");
    }

    private record CourtFigures(DashboardStats.Registrar stats, List<HearingRow> today) {
    }

    /** Loads today's figures and cause list (quietly skipped if the database cannot be reached). */
    private void loadCourtFigures() {
        BackgroundTask.run(
                () -> new CourtFigures(dashboardDao.registrarStats(), dashboardDao.todaysHearings()),
                figures -> {
                    DashboardStats.Registrar stats = figures.stats();
                    pendingStatLabel.setText(String.valueOf(stats.pendingCases()));
                    hearingsStatLabel.setText(String.valueOf(stats.hearingsToday()));
                    disposedStatLabel.setText(String.valueOf(stats.disposedLast30Days()));
                    liveStatsBox.setVisible(true);
                    liveStatsBox.setManaged(true);
                    showNextHearings(figures.today());
                },
                error -> showNextHearings(List.of()));
    }

    private void showNextHearings(List<HearingRow> today) {
        causeListBox.getChildren().clear();
        LocalTime now = LocalTime.now();
        // Streams: keep hearings that have not started yet, take the first three
        List<HearingRow> next = today.stream()
                .filter(h -> !h.time().isBefore(now.withSecond(0).withNano(0)))
                .limit(3)
                .toList();
        if (next.isEmpty()) {
            Label none = new Label(today.isEmpty() ? "No hearings listed for today." : "All of today's hearings have been called.");
            none.getStyleClass().add("cause-empty");
            causeListBox.getChildren().add(none);
            return;
        }
        for (HearingRow h : next) {
            Label left = new Label(h.time().format(TIME).toLowerCase() + "   " + h.caseNumber());
            left.getStyleClass().add("cause-case");
            Label right = new Label("Room " + h.roomNo());
            right.getStyleClass().add("cause-room");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(left, spacer, right);
            row.getStyleClass().add("cause-row");
            causeListBox.getChildren().add(row);
        }
    }

    // ------------------------------------------------------------------ sign in

    /** Event handler for the Sign in button and the Enter key in the password field. */
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
            markError(usernameGroup);
            ok = false;
        }
        if (password.isEmpty()) {
            markError(passwordGroup);
            ok = false;
        }
        if (!ok) {
            showError("Enter both your username and password.");
        }
        return ok;
    }

    private void onLoginSuccess(User user) {
        setBusy(false);

        // The account must match the tab the user picked
        if (user.getRole() != selectedRole()) {
            passwordField.clear();
            showError("This is a " + user.getRole().getDisplayName() + " account. Choose the "
                    + user.getRole().getDisplayName() + " tab to sign in.");
            return;
        }

        SessionManager.login(user);
        if (rememberMeCheck.isSelected()) {
            RememberMeStore.save(user.getUsername(), user.getRole());
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
            markError(passwordGroup);
        } else {
            showError("Couldn't reach the database. Check that MySQL is running.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        AlertUtil.info("Forgot password",
                "For security, passwords are reset by the Registrar's office.\n"
                        + "Ask the Registrar to set a new password for your account.");
    }

    // ------------------------------------------------------------------ public case look-up

    @FXML
    private void handleCaseStatus() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Check case status");
        dialog.setHeaderText(null);
        dialog.setContentText("Case number");
        dialog.getEditor().setPromptText("CS/101/2026");
        styleDialog(dialog);

        Optional<String> entered = dialog.showAndWait();
        entered.map(String::trim).filter(s -> !s.isEmpty()).ifPresent(caseNumber ->
                BackgroundTask.run(
                        () -> dashboardDao.findPublicStatus(caseNumber),
                        found -> found.ifPresentOrElse(
                                this::showCaseResult,
                                () -> AlertUtil.warning("Case not found",
                                        "No case with number " + caseNumber.toUpperCase()
                                                + " was found. Check the number and try again."))));
    }

    /** Shows the case details in a small dialog laid out with a GridPane. */
    private void showCaseResult(CaseSummary c) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(10);
        addRow(grid, 0, "Case number", value(c.caseNumber()));
        addRow(grid, 1, "Status", UiFactory.statusChip(c.status()));
        addRow(grid, 2, "Case type", value(c.type().getDisplayName()));
        addRow(grid, 3, "Next hearing", value(c.date() == null ? "Not scheduled"
                : c.date().format(DATE) + "  (" + UiFactory.relativeDays(c.date()) + ")"));
        addRow(grid, 4, "Court", value(c.note() == null ? "Not yet assigned" : c.note()));

        Label title = new Label(c.title());
        title.getStyleClass().add("result-title");
        title.setWrapText(true);
        VBox content = new VBox(16, title, grid);
        content.setMinWidth(380);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Case status");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        styleDialog(dialog);
        dialog.showAndWait();
    }

    private void addRow(GridPane grid, int row, String key, Node valueNode) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("result-key");
        grid.add(keyLabel, 0, row);
        grid.add(valueNode, 1, row);
    }

    private Label value(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("result-value");
        label.setWrapText(true);
        return label;
    }

    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(SceneManager.css());
        dialog.initOwner(SceneManager.getStage());
    }

    // ------------------------------------------------------------------ UI state helpers

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Signing in…" : "Sign in");
        if (busy) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(16, 16);
            loginButton.setGraphic(spinner);
            hideError();
        } else {
            loginButton.setGraphic(null);
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

    private void markError(Node group) {
        if (!group.getStyleClass().contains(ERROR_STYLE)) {
            group.getStyleClass().add(ERROR_STYLE);
        }
    }

    private void clearError(Node group) {
        group.getStyleClass().remove(ERROR_STYLE);
    }
}
