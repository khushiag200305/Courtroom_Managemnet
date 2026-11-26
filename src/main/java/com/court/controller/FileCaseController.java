package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.CaseDocument;
import com.court.model.CaseType;
import com.court.model.CourtCase;
import com.court.model.Lawyer;
import com.court.model.Party;
import com.court.model.PartyType;
import com.court.service.CaseService;
import com.court.service.DocumentService;
import com.court.ui.Notifier;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lawyer's filing form (file_case.fxml). With {@code toCorrect} set, the same form
 * corrects a filing that the Registrar returned, and files it again.
 */
public class FileCaseController {

    private static final int MAX_DETAILS = 2000;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox defectBanner;
    @FXML private Label defectLabel;
    @FXML private ComboBox<CaseType> typeBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label charCount;
    @FXML private CheckBox urgentCheck;
    @FXML private VBox petitionerBox;
    @FXML private VBox respondentBox;
    @FXML private VBox dropZone;
    @FXML private VBox existingBox;
    @FXML private VBox fileList;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

    private final CaseService caseService = new CaseService();
    private final DocumentService documentService = new DocumentService();
    private final List<PartyForm> petitioners = new ArrayList<>();
    private final List<PartyForm> respondents = new ArrayList<>();
    private final List<Path> files = new ArrayList<>();

    private Lawyer lawyer;
    private CourtCase toCorrect;
    private Runnable onDone = () -> { };
    private Runnable onCancel = () -> { };

    /** The fields of one party card. Inner class: it uses the controller's lists directly. */
    private final class PartyForm {
        final PartyType type;
        final TextField name = new TextField();
        final TextField address = new TextField();
        final TextField phone = new TextField();
        final VBox node;

        PartyForm(PartyType type, Party existing) {
            this.type = type;
            name.setPromptText(type == PartyType.PETITIONER ? "Full name of the petitioner" : "Full name of the respondent");
            address.setPromptText("Address");
            phone.setPromptText("Mobile");
            phone.setPrefWidth(130);
            if (existing != null) {
                name.setText(existing.name());
                address.setText(existing.address() == null ? "" : existing.address());
                phone.setText(existing.phone() == null ? "" : existing.phone());
            }
            Button remove = UiFactory.rowButton("fth-x", "Remove", this::remove);
            HBox top = new HBox(8, name, remove);
            HBox.setHgrow(name, Priority.ALWAYS);
            HBox bottom = new HBox(8, address, phone);
            HBox.setHgrow(address, Priority.ALWAYS);
            node = new VBox(8, top, bottom);
            node.getStyleClass().add("party-card");
        }

        void remove() {
            List<PartyForm> list = type == PartyType.PETITIONER ? petitioners : respondents;
            if (list.size() == 1) {
                showError("A case needs at least one " + type.getDisplayName().toLowerCase() + ".");
                return;
            }
            list.remove(this);
            (type == PartyType.PETITIONER ? petitionerBox : respondentBox).getChildren().remove(node);
        }

        Party toParty() {
            return new Party(name.getText(), type, address.getText(), phone.getText());
        }
    }

    @FXML
    private void initialize() {
        typeBox.getItems().setAll(CaseType.values());
        typeBox.setMaxWidth(Double.MAX_VALUE);
        descriptionArea.textProperty().addListener((obs, old, text) -> {
            if (text.length() > MAX_DETAILS) {
                descriptionArea.setText(text.substring(0, MAX_DETAILS));
            }
            charCount.setText(descriptionArea.getText().length() + " / " + MAX_DETAILS + " characters");
        });
        charCount.setText("0 / " + MAX_DETAILS + " characters");
        setUpDragAndDrop();
    }

    /**
     * @param toCorrect a returned (defective) filing to correct, or null for a new case
     */
    public void init(Lawyer lawyer, CourtCase toCorrect, Runnable onDone, Runnable onCancel) {
        this.lawyer = lawyer;
        this.toCorrect = toCorrect;
        this.onDone = onDone;
        this.onCancel = onCancel;

        if (toCorrect == null) {
            addParty(PartyType.PETITIONER, null);
            addParty(PartyType.RESPONDENT, null);
            return;
        }

        titleLabel.setText("Correct and refile");
        subtitleLabel.setText(toCorrect.getTitle() + ". Fix the points below, add any missing documents, then file again.");
        defectLabel.setText("Returned by the Registrar: " + toCorrect.getDefectReason());
        defectBanner.setVisible(true);
        defectBanner.setManaged(true);
        submitButton.setText("File again");

        CourtCase full = caseService.findWithParties(toCorrect.getId()).orElse(toCorrect);
        typeBox.setValue(full.getType());
        descriptionArea.setText(full.getDescription());
        urgentCheck.setSelected(full.isUrgent());
        full.getParties().forEach(p -> addParty(p.type(), p));
        if (petitioners.isEmpty()) addParty(PartyType.PETITIONER, null);
        if (respondents.isEmpty()) addParty(PartyType.RESPONDENT, null);

        List<CaseDocument> existing = documentService.findByCase(toCorrect.getId());
        if (!existing.isEmpty()) {
            Label heading = new Label("Already attached");
            heading.getStyleClass().add("section-title");
            existingBox.getChildren().add(heading);
            existing.forEach(d -> existingBox.getChildren().add(UiFactory.listRow(UiFactory.icon("fth-paperclip", 16),
                    d.fileName(), d.readableSize(), UiFactory.chip("Attached", "chip-green"))));
        }
    }

    // ------------------------------------------------------------------ parties

    @FXML
    private void handleAddPetitioner() {
        addParty(PartyType.PETITIONER, null);
    }

    @FXML
    private void handleAddRespondent() {
        addParty(PartyType.RESPONDENT, null);
    }

    private void addParty(PartyType type, Party existing) {
        PartyForm form = new PartyForm(type, existing);
        if (type == PartyType.PETITIONER) {
            petitioners.add(form);
            petitionerBox.getChildren().add(form.node);
        } else {
            respondents.add(form);
            respondentBox.getChildren().add(form.node);
        }
    }

    // ------------------------------------------------------------------ documents

    private void setUpDragAndDrop() {
        dropZone.setOnDragOver((DragEvent event) -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZone.getStyleClass().contains("drag-over")) dropZone.getStyleClass().add("drag-over");
            }
            event.consume();
        });
        dropZone.setOnDragExited(event -> dropZone.getStyleClass().remove("drag-over"));
        dropZone.setOnDragDropped(event -> {
            boolean ok = event.getDragboard().hasFiles();
            if (ok) {
                event.getDragboard().getFiles().forEach(f -> addFile(f.toPath()));
            }
            event.setDropCompleted(ok);
            event.consume();
        });
    }

    @FXML
    private void handleChooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose documents");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Court documents", "*.pdf", "*.jpg", "*.jpeg", "*.png",
                        "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        List<File> chosen = chooser.showOpenMultipleDialog(SceneManager.getStage());
        if (chosen != null) {
            chosen.forEach(f -> addFile(f.toPath()));
        }
    }

    private void addFile(Path file) {
        try {
            if (files.contains(file)) return;
            if (files.size() >= DocumentService.MAX_FILES) {
                throw new ValidationException("You can attach at most " + DocumentService.MAX_FILES + " files.");
            }
            documentService.check(file);
            files.add(file);
            hideError();
            renderFiles();
        } catch (ValidationException e) {
            showError(e.getMessage());
        }
    }

    private void renderFiles() {
        fileList.getChildren().clear();
        for (Path file : files) {
            String size;
            try {
                long bytes = Files.size(file);
                size = bytes < 1024 * 1024 ? Math.max(1, bytes / 1024) + " KB" : String.format("%.1f MB", bytes / 1048576.0);
            } catch (IOException e) {
                size = "";
            }
            Button remove = UiFactory.rowButton("fth-trash-2", "Remove", () -> {
                files.remove(file);
                renderFiles();
            });
            fileList.getChildren().add(UiFactory.listRow(UiFactory.icon("fth-file", 16),
                    file.getFileName().toString(), size + "  ·  will be uploaded", remove));
        }
    }

    // ------------------------------------------------------------------ submit

    @FXML
    private void handleSubmit() {
        hideError();
        List<Party> parties = new ArrayList<>();
        petitioners.forEach(p -> parties.add(p.toParty()));
        respondents.forEach(p -> parties.add(p.toParty()));
        CaseService.FilingForm form = new CaseService.FilingForm(typeBox.getValue(), descriptionArea.getText(),
                urgentCheck.isSelected(), parties, List.copyOf(files));

        submitButton.setDisable(true);
        submitButton.setText("Submitting…");
        BackgroundTask.run(() -> {
            if (toCorrect == null) {
                return caseService.fileCase(lawyer, form).getTitle();
            }
            caseService.resubmit(toCorrect, lawyer, form);
            return toCorrect.getTitle();
        }, title -> {
            Notifier.success((toCorrect == null ? "Filed: " : "Filed again: ") + title);
            onDone.run();
        }, error -> {
            submitButton.setDisable(false);
            submitButton.setText(toCorrect == null ? "Submit filing" : "File again");
            showError(error instanceof ValidationException ? error.getMessage()
                    : "The filing could not be saved: " + error.getMessage());
        });
    }

    @FXML
    private void handleCancel() {
        onCancel.run();
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
}
