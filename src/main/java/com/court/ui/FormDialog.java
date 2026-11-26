package com.court.ui;

import com.court.exception.DataAccessException;
import com.court.exception.ValidationException;
import com.court.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

/**
 * A dialog with a two-column form (labels on the left, fields on the right) laid out in a GridPane.
 * <p>
 * When the user clicks the save button, the {@link SaveAction} runs. If it throws a
 * {@link ValidationException}, the message is shown in red inside the dialog and the dialog
 * stays open, so the user can fix the input without retyping everything.
 */
public class FormDialog {

    /** The work to do when the user clicks save; throws to keep the dialog open. */
    @FunctionalInterface
    public interface SaveAction {
        void save() throws ValidationException;
    }

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final Label errorLabel = new Label();
    private final ButtonType saveButton;
    private int row = 0;

    public FormDialog(String title, String subtitle, String saveText) {
        dialog.setTitle(title);
        dialog.initOwner(SceneManager.getStage());

        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);

        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(130);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(300);
        grid.getColumnConstraints().addAll(labels, fields);

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setGraphic(new FontIcon("fth-alert-circle"));
        showError(null);

        VBox content = new VBox(14, new VBox(4, heading, sub), grid, errorLabel);
        content.setPadding(new Insets(8, 6, 4, 6));
        content.setPrefWidth(520);

        saveButton = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButton);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(SceneManager.css());
    }

    /** Adds a labelled row and returns the field, so it can be created and added in one line. */
    public <T extends Node> T add(String label, T field) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        grid.add(l, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        row++;
        return field;
    }

    /** Adds a node across both columns (e.g. a note or a list). */
    public <T extends Node> T addWide(T node) {
        grid.add(node, 0, row, 2, 1);
        row++;
        return node;
    }

    public void addNote(String text) {
        Label note = new Label(text);
        note.getStyleClass().add("hint-label");
        note.setWrapText(true);
        addWide(note);
    }

    public Dialog<ButtonType> getDialog() {
        return dialog;
    }

    /**
     * Shows the dialog. Returns true if the save action succeeded, false if the user cancelled.
     */
    public boolean showAndSave(SaveAction action) {
        Node save = dialog.getDialogPane().lookupButton(saveButton);
        // An event filter runs before the dialog closes; consuming the event keeps it open
        save.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                action.save();
            } catch (ValidationException | DataAccessException e) {
                showError(e.getMessage());
                event.consume();
            }
        });
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == saveButton;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(message != null);
        errorLabel.setManaged(message != null);
        if (message != null) {
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        }
    }
}
