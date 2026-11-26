package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.CaseStatus;
import com.court.model.Hearing;
import com.court.model.HearingStatus;
import com.court.model.Judge;
import com.court.model.OrderOutcome;
import com.court.service.HearingService;
import com.court.ui.CaseDetailsDialog;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.SlotPicker;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Judge's cause list for a day (judge_cause_list.fxml) with "Record order".
 */
public class JudgeCauseListController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");

    /** Ready-made order texts the judge can start from and then edit. */
    private static final List<String> TEMPLATES = List.of(
            "Notice issued to the respondent, returnable on the next date.",
            "Reply not filed. Last opportunity granted to the respondent to file reply.",
            "Counsel for the petitioner seeks time. Adjourned at the request of the petitioner.",
            "Evidence of the petitioner partly recorded. Remaining evidence on the next date.",
            "Arguments heard in part. Matter to come up for further arguments.",
            "Final arguments heard. Judgment reserved.");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private DatePicker datePicker;
    @FXML private Label progressLabel;
    @FXML private TableView<Hearing> table;
    @FXML private TableColumn<Hearing, String> srCol;
    @FXML private TableColumn<Hearing, String> timeCol;
    @FXML private TableColumn<Hearing, String> caseNoCol;
    @FXML private TableColumn<Hearing, String> titleCol;
    @FXML private TableColumn<Hearing, CaseStatus> stageCol;
    @FXML private TableColumn<Hearing, String> hearingCol;
    @FXML private TableColumn<Hearing, Void> actionsCol;

    private final HearingService hearingService = new HearingService();
    private Judge judge;

    @FXML
    private void initialize() {
        UiFactory.pageTable(table, "No cases are listed before you on this day.");
        UiFactory.textColumn(srCol, h -> String.valueOf(table.getItems().indexOf(h) + 1));
        UiFactory.textColumn(timeCol, h -> h.time().format(TIME).toLowerCase());
        UiFactory.textColumn(caseNoCol, Hearing::caseNumber);
        caseNoCol.getStyleClass().add("mono-cell");
        UiFactory.textColumn(titleCol, Hearing::caseTitle);
        UiFactory.statusColumn(stageCol, Hearing::caseStatus);
        UiFactory.chipColumn(hearingCol, h -> h.status() == HearingStatus.COMPLETED ? "Order passed"
                : h.status().getDisplayName(), h -> CaseDetailsDialog.hearingStyle(h.status()));
        UiFactory.actionColumn(actionsCol, this::rowButtons);
        UiFactory.onDoubleClick(table, h -> CaseDetailsDialog.show(h.caseId()));
        datePicker.valueProperty().addListener((o, a, b) -> load());
    }

    /** Called after loading: whose cause list to show. */
    public void init(Judge judge) {
        this.judge = judge;
        subtitleLabel.setText(judge.getName() + (judge.hasCourtroom() ? "  ·  Room " + judge.getCourtroomName() : ""));
        datePicker.setValue(LocalDate.now());
    }

    private List<Button> rowButtons(Hearing h) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(UiFactory.rowButton("fth-folder", "Open case file", () -> CaseDetailsDialog.show(h.caseId())));
        if (h.status() == HearingStatus.SCHEDULED && !h.date().isAfter(LocalDate.now())) {
            Button record = UiFactory.button("Record order", "fth-edit-3", "row-action", () -> recordOrder(h));
            buttons.add(record);
        }
        return buttons;
    }

    private void load() {
        LocalDate day = datePicker.getValue();
        if (day == null || judge == null) return;
        titleLabel.setText(day.equals(LocalDate.now()) ? "Today's cause list" : "Cause list");
        BackgroundTask.run(() -> hearingService.forJudge(judge.getId(), day, day), list -> {
            table.getItems().setAll(list);
            long done = list.stream().filter(h -> h.status() == HearingStatus.COMPLETED).count();
            progressLabel.setText(day.format(LONG) + "   ·   " + done + " of " + list.size() + " heard");
        });
    }

    @FXML
    private void handlePrevious() {
        datePicker.setValue(datePicker.getValue().minusDays(1));
    }

    @FXML
    private void handleNext() {
        datePicker.setValue(datePicker.getValue().plusDays(1));
    }

    @FXML
    private void handleToday() {
        datePicker.setValue(LocalDate.now());
    }

    // ------------------------------------------------------------------ record order

    private void recordOrder(Hearing h) {
        FormDialog form = new FormDialog("Record order", h.caseNumber() + "  ·  " + h.caseTitle(), "Pass order");

        ComboBox<String> template = form.add("Start from", new ComboBox<>());
        template.getItems().setAll(TEMPLATES);
        template.setPromptText("A common order (optional)");
        template.setMaxWidth(Double.MAX_VALUE);

        TextArea text = form.add("Order", new TextArea());
        text.setPromptText("What happened at this hearing and what the parties must do next");
        text.setWrapText(true);
        text.setPrefRowCount(5);
        template.valueProperty().addListener((o, a, chosen) -> {
            if (chosen != null) text.setText(chosen);
        });

        ComboBox<OrderOutcome> outcome = form.add("Outcome", new ComboBox<>());
        outcome.getItems().setAll(OrderOutcome.values());
        outcome.setValue(OrderOutcome.NEXT_HEARING);
        outcome.setMaxWidth(Double.MAX_VALUE);

        SlotPicker next = new SlotPicker(hearingService, h.date().plusDays(1).isAfter(LocalDate.now())
                ? h.date().plusDays(1) : LocalDate.now().plusDays(1));
        next.datePicker().setValue(SlotPicker.nextOpenDay(LocalDate.now().plusDays(14), hearingService.holidays()));
        VBox nextBox = form.add("Next hearing", next.asNode());
        next.setParticipants(() -> new SlotPicker.Participants(h.judgeId(), h.courtroomId(), h.lawyerId()));

        Label note = form.add("", new Label());
        note.getStyleClass().add("hint-label");
        note.setWrapText(true);

        // Listener: show the next-date fields only when the outcome needs them
        outcome.valueProperty().addListener((o, a, chosen) -> {
            boolean needsDate = chosen != null && chosen.needsNextDate();
            nextBox.setVisible(needsDate);
            nextBox.setManaged(needsDate);
            note.setText(chosen == OrderOutcome.DISPOSED
                    ? "This closes the case. The order text is recorded as the final judgment."
                    : chosen == OrderOutcome.RESERVED
                    ? "The case leaves the cause list until you deliver judgment from Reserved judgments."
                    : "");
            form.getDialog().getDialogPane().getScene().getWindow().sizeToScene();
        });

        if (form.showAndSave(() -> {
            if (outcome.getValue() == null) throw new ValidationException("Choose the outcome.");
            hearingService.recordOrder(h, judge.getId(), text.getText(), outcome.getValue(), next.date(), next.time());
        })) {
            OrderOutcome result = outcome.getValue();
            Notifier.success(switch (result) {
                case DISPOSED -> "Case disposed";
                case RESERVED -> "Judgment reserved";
                default -> "Order passed · next hearing " + next.date().format(DateTimeFormatter.ofPattern("d MMM"));
            });
            load();
        }
    }
}
