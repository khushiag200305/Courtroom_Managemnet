package com.court.controller;

import com.court.model.CaseStatus;
import com.court.model.CourtCase;
import com.court.model.Judge;
import com.court.model.Lawyer;
import com.court.model.User;
import com.court.service.CaseService;
import com.court.ui.CaseDetailsDialog;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.TableFilter;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A judge's or a lawyer's own cases (cases.fxml). The same screen also shows
 * "Reserved judgments" (judge) and "Returned filings" (lawyer) with a preset stage filter.
 */
public class CasesController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM");
    private static final String ALL = "All stages";

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox headerActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> stageFilter;
    @FXML private Label countLabel;
    @FXML private TableView<CourtCase> table;
    @FXML private TableColumn<CourtCase, String> caseNoCol;
    @FXML private TableColumn<CourtCase, String> titleCol;
    @FXML private TableColumn<CourtCase, String> typeCol;
    @FXML private TableColumn<CourtCase, CaseStatus> stageCol;
    @FXML private TableColumn<CourtCase, String> nextCol;
    @FXML private TableColumn<CourtCase, String> otherCol;
    @FXML private TableColumn<CourtCase, Void> actionsCol;

    private final CaseService caseService = new CaseService();
    private TableFilter<CourtCase> filter;
    private User user;
    /** What to do when a lawyer clicks "Correct and refile" (opens the filing form). */
    private Consumer<CourtCase> onCorrect = c -> { };

    @FXML
    private void initialize() {
        stageFilter.getItems().add(ALL);
        for (CaseStatus s : CaseStatus.values()) {
            stageFilter.getItems().add(s.getDisplayName());
        }
        stageFilter.setValue(ALL);
        UiFactory.pageTable(table, "No cases here.");
    }

    /**
     * @param preset stage to show first (e.g. RESERVED for "Reserved judgments"), or null for all
     */
    public void init(User user, CaseStatus preset, String title, String subtitle) {
        this.user = user;
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
        boolean isJudge = user instanceof Judge;
        otherCol.setText(isJudge ? "Filed by" : "Judge");

        UiFactory.textColumn(caseNoCol, CourtCase::displayNumber);
        caseNoCol.getStyleClass().add("mono-cell");
        UiFactory.textColumn(titleCol, c -> (c.isUrgent() ? "● " : "") + c.getTitle());
        UiFactory.textColumn(typeCol, c -> c.getType().getDisplayName());
        UiFactory.statusColumn(stageCol, CourtCase::getStatus);
        UiFactory.textColumn(nextCol, c -> c.getNextHearingDate() == null ? "-" : c.getNextHearingDate().format(DATE));
        UiFactory.textColumn(otherCol, c -> isJudge ? c.getFiledByName()
                : (c.getJudgeName() == null ? "Not assigned" : c.getJudgeName()));
        UiFactory.actionColumn(actionsCol, this::rowButtons);
        UiFactory.onDoubleClick(table, c -> CaseDetailsDialog.show(c.getId()));

        filter = new TableFilter<>(table, searchField, c -> c.displayNumber() + " " + c.getTitle()
                + " " + c.getFiledByName() + " " + c.getJudgeName());
        filter.bindCount(countLabel, "cases");
        stageFilter.valueProperty().addListener((o, a, stage) ->
                filter.setExtra(c -> ALL.equals(stage) || c.getStatus().getDisplayName().equals(stage)));
        stageFilter.setValue(preset == null ? ALL : preset.getDisplayName());
        load();
    }

    public void setOnCorrect(Consumer<CourtCase> onCorrect) {
        this.onCorrect = onCorrect;
    }

    /** Lets the dashboard put a button (e.g. "File a new case") in the page header. */
    public void addHeaderButton(Button button) {
        headerActions.getChildren().add(button);
    }

    private void load() {
        BackgroundTask.run(() -> user instanceof Judge
                ? caseService.findByJudge(user.getId())
                : caseService.findByLawyer(user.getId()), cases -> filter.setItems(cases));
    }

    private List<Button> rowButtons(CourtCase c) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(UiFactory.rowButton("fth-folder", "Open case file", () -> CaseDetailsDialog.show(c.getId())));
        if (user instanceof Judge && (c.getStatus() == CaseStatus.RESERVED)) {
            buttons.add(UiFactory.button("Deliver judgment", "mdi2s-scale-balance", "row-action", () -> deliverJudgment(c)));
        }
        if (user instanceof Lawyer && c.getStatus() == CaseStatus.DEFECTIVE) {
            buttons.add(UiFactory.button("Correct and refile", "fth-edit-2", "row-action", () -> onCorrect.accept(c)));
        }
        return buttons;
    }

    private void deliverJudgment(CourtCase c) {
        FormDialog form = new FormDialog("Deliver judgment", c.getCaseNumber() + "  ·  " + c.getTitle(),
                "Deliver judgment");
        TextArea text = form.add("Judgment", new TextArea());
        text.setPromptText("Findings, the decision, and any directions (costs, compliance period ...)");
        text.setWrapText(true);
        text.setPrefRowCount(10);
        form.addNote("Delivering the judgment disposes of the case and cancels any hearings still listed.");
        if (form.showAndSave(() -> caseService.deliverJudgment(c, user.getId(), text.getText()))) {
            Notifier.success("Judgment delivered in " + c.getCaseNumber());
            load();
        }
    }
}
