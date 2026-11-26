package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.Courtroom;
import com.court.model.Judge;
import com.court.model.Lawyer;
import com.court.model.Role;
import com.court.model.User;
import com.court.model.UserFactory;
import com.court.service.CourtroomService;
import com.court.service.UserService;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.TableFilter;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registrar screen for managing judge or lawyer accounts (users.fxml).
 */
public class UsersController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button importButton;
    @FXML private Button addButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> detailCol;
    @FXML private TableColumn<User, String> extraCol;
    @FXML private TableColumn<User, String> phoneCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionsCol;

    private final UserService userService = new UserService();
    private final CourtroomService courtroomService = new CourtroomService();
    private TableFilter<User> filter;
    private Role role;

    @FXML
    private void initialize() {
        statusFilter.getItems().setAll("All accounts", "Active", "Inactive");
        statusFilter.getSelectionModel().selectFirst();
        UiFactory.pageTable(table, "No accounts yet.");
    }

    /** Called right after the screen is loaded: decides whether it shows judges or lawyers. */
    public void init(Role role) {
        this.role = role;
        boolean judges = role == Role.JUDGE;
        titleLabel.setText(judges ? "Judges" : "Lawyers");
        subtitleLabel.setText(judges ? "Judicial officers and the courtrooms they sit in."
                : "Advocates enrolled to file cases in this court.");
        addButton.setText(judges ? "Add judge" : "Add lawyer");
        importButton.setVisible(!judges);
        importButton.setManaged(!judges);
        detailCol.setText(judges ? "Designation" : "Bar Council ID");
        extraCol.setText(judges ? "Courtroom" : "Email");

        UiFactory.textColumn(nameCol, User::getName);
        UiFactory.textColumn(usernameCol, User::getUsername);
        UiFactory.textColumn(detailCol, u -> u instanceof Judge j ? j.getDesignation() : ((Lawyer) u).getBarId());
        UiFactory.textColumn(extraCol, u -> u instanceof Judge j
                ? (j.hasCourtroom() ? "Room " + j.getCourtroomName() : "Not assigned")
                : Objects.requireNonNullElse(u.getEmail(), "-"));
        UiFactory.textColumn(phoneCol, u -> Objects.requireNonNullElse(u.getPhone(), "-"));
        UiFactory.chipColumn(statusCol, u -> u.isActive() ? "Active" : "Inactive",
                u -> u.isActive() ? "chip-green" : "chip-neutral");
        UiFactory.actionColumn(actionsCol, u -> List.of(
                UiFactory.rowButton("fth-edit-2", "Edit", () -> openForm(u)),
                UiFactory.rowButton("fth-key", "Reset password", () -> resetPassword(u)),
                UiFactory.rowButton(u.isActive() ? "fth-user-x" : "fth-user-check",
                        u.isActive() ? "Deactivate" : "Activate", () -> toggleActive(u)),
                UiFactory.rowButton("fth-trash-2", "Delete", () -> delete(u))));
        UiFactory.onDoubleClick(table, this::openForm);

        filter = new TableFilter<>(table, searchField, u -> u.getName() + " " + u.getUsername() + " "
                + (u instanceof Lawyer l ? l.getBarId() : ((Judge) u).getDesignation()));
        filter.bindCount(countLabel, judges ? "judges" : "lawyers");
        statusFilter.valueProperty().addListener((obs, oldValue, value) -> filter.setExtra(u ->
                "Active".equals(value) ? u.isActive() : !"Inactive".equals(value) || !u.isActive()));
        load();
    }

    private void load() {
        BackgroundTask.run(() -> userService.findByRole(role), users -> filter.setItems(users));
    }

    // ------------------------------------------------------------------ add / edit

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    private void openForm(User existing) {
        boolean isNew = existing == null;
        String noun = role == Role.JUDGE ? "judge" : "lawyer";
        FormDialog form = new FormDialog(isNew ? "Add " + noun : "Edit " + noun,
                isNew ? "The " + noun + " can sign in as soon as the account is created."
                        : "Changes apply the next time they sign in.",
                isNew ? "Add " + noun : "Save changes");

        TextField name = form.add("Full name", field(isNew ? "" : existing.getName(),
                role == Role.JUDGE ? "Justice Meera Kapoor" : "Adv. Neha Verma"));
        TextField username = form.add("Username", field(isNew ? "" : existing.getUsername(), "judge5"));
        username.setDisable(!isNew);
        PasswordField password = new PasswordField();
        if (isNew) {
            password.setPromptText("At least 6 characters");
            form.add("Password", password);
        }
        TextField email = form.add("Email", field(isNew ? "" : existing.getEmail(), "name@example.com"));
        TextField phone = form.add("Mobile", field(isNew ? "" : existing.getPhone(), "9876543210"));

        TextField designation = new TextField();
        ComboBox<Courtroom> room = new ComboBox<>();
        TextField barId = new TextField();
        if (role == Role.JUDGE) {
            designation.setText(isNew ? "" : ((Judge) existing).getDesignation());
            designation.setPromptText("Civil Judge (Senior Division)");
            form.add("Designation", designation);
            room.getItems().setAll(courtroomService.findActive());
            room.setPromptText("Not assigned");
            room.setMaxWidth(Double.MAX_VALUE);
            if (!isNew && ((Judge) existing).hasCourtroom()) {
                room.getItems().stream().filter(r -> r.getId() == ((Judge) existing).getCourtroomId())
                        .findFirst().ifPresent(room::setValue);
            }
            form.add("Courtroom", room);
            form.addNote("Each courtroom can have only one judge.");
        } else {
            barId.setText(isNew ? "" : ((Lawyer) existing).getBarId());
            barId.setPromptText("PB/1021/2014");
            form.add("Bar Council ID", barId);
        }

        boolean saved = form.showAndSave(() -> {
            User user = isNew ? UserFactory.create(role) : existing;
            user.setName(name.getText());
            if (isNew) user.setUsername(username.getText());
            user.setEmail(email.getText());
            user.setPhone(phone.getText());
            if (user instanceof Judge judge) {
                judge.setDesignation(designation.getText());
                judge.setCourtroomId(room.getValue() == null ? null : room.getValue().getId());
            } else if (user instanceof Lawyer lawyer) {
                lawyer.setBarId(barId.getText());
            }
            userService.save(user, password.getText());
        });
        if (saved) {
            Notifier.success(isNew ? capital(noun) + " added" : "Changes saved");
            load();
        } else if (!isNew) {
            load();   // undo any half-applied edits shown in the table
        }
    }

    // ------------------------------------------------------------------ row actions

    private void resetPassword(User user) {
        FormDialog form = new FormDialog("Reset password", "Set a new password for " + user.getName() + ".",
                "Reset password");
        PasswordField first = form.add("New password", new PasswordField());
        PasswordField second = form.add("Repeat password", new PasswordField());
        if (form.showAndSave(() -> {
            if (!first.getText().equals(second.getText())) {
                throw new ValidationException("The two passwords do not match.");
            }
            userService.resetPassword(user, first.getText());
        })) {
            Notifier.success("Password changed for " + user.getUsername());
        }
    }

    private void toggleActive(User user) {
        boolean activate = !user.isActive();
        String verb = activate ? "Activate" : "Deactivate";
        if (AlertUtil.confirm(verb + " account", verb + " " + user.getName() + "?"
                + (activate ? "" : "\nThey will not be able to sign in until the account is activated again."))) {
            userService.setActive(user, activate);
            Notifier.success(user.getName() + (activate ? " can sign in again" : " deactivated"));
            load();
        }
    }

    private void delete(User user) {
        if (!AlertUtil.confirm("Delete account", "Delete " + user.getName() + " permanently?\n"
                + "Accounts linked to cases cannot be deleted - deactivate them instead.")) {
            return;
        }
        try {
            userService.delete(user);
            Notifier.success("Account deleted");
            load();
        } catch (ValidationException e) {
            AlertUtil.warning("Can't delete this account", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ CSV import

    @FXML
    private void handleImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a CSV file of lawyers");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(SceneManager.getStage());
        if (file == null) return;

        BackgroundTask.run(() -> userService.importLawyers(file.toPath()), result -> {
            load();
            List<String> lines = new ArrayList<>();
            lines.add(result.imported() + " lawyer(s) imported. Their password is \""
                    + UserService.DEFAULT_IMPORT_PASSWORD + "\" unless the file gave one.");
            if (!result.problems().isEmpty()) {
                lines.add("");
                lines.add(result.problems().size() + " line(s) skipped:");
                lines.addAll(result.problems());
            }
            TextArea text = new TextArea(String.join("\n", lines));
            text.setEditable(false);
            text.setWrapText(true);
            text.setPrefSize(520, 260);
            Label hint = new Label("Expected columns: name,username,bar_id,email,phone[,password]");
            hint.getStyleClass().add("hint-label");

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Import finished");
            dialog.initOwner(SceneManager.getStage());
            dialog.getDialogPane().setContent(new VBox(10, text, hint));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().getStylesheets().add(SceneManager.css());
            dialog.showAndWait();
        });
    }

    private static TextField field(String value, String prompt) {
        TextField f = new TextField(value == null ? "" : value);
        f.setPromptText(prompt);
        return f;
    }

    private static String capital(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
