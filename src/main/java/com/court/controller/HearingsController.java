package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.CaseStatus;
import com.court.model.Courtroom;
import com.court.model.CourtCase;
import com.court.model.Hearing;
import com.court.model.HearingStatus;
import com.court.service.CaseService;
import com.court.service.CourtroomService;
import com.court.service.HearingService;
import com.court.ui.CaseDetailsDialog;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.SlotPicker;
import com.court.ui.TableFilter;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Registrar's hearing diary (hearings.fxml).
 */
public class HearingsController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String ALL_ROOMS = "All courtrooms";

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<String> roomFilter;
    @FXML private CheckBox showCancelled;
    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Hearing> table;
    @FXML private TableColumn<Hearing, String> dateCol;
    @FXML private TableColumn<Hearing, String> timeCol;
    @FXML private TableColumn<Hearing, String> caseNoCol;
    @FXML private TableColumn<Hearing, String> titleCol;
    @FXML private TableColumn<Hearing, String> roomCol;
    @FXML private TableColumn<Hearing, String> judgeCol;
    @FXML private TableColumn<Hearing, String> statusCol;
    @FXML private TableColumn<Hearing, Void> actionsCol;

    private final HearingService hearingService = new HearingService();
    private final CaseService caseService = new CaseService();
    private final CourtroomService courtroomService = new CourtroomService();
    private TableFilter<Hearing> filter;

    @FXML
    private void initialize() {
        UiFactory.pageTable(table, "No hearings in this period.");
        UiFactory.textColumn(dateCol, h -> h.date().format(DATE));
        UiFactory.textColumn(timeCol, h -> h.time().format(TIME).toLowerCase());
        UiFactory.textColumn(caseNoCol, Hearing::caseNumber);
        caseNoCol.getStyleClass().add("mono-cell");
        UiFactory.textColumn(titleCol, Hearing::caseTitle);
        UiFactory.textColumn(roomCol, Hearing::roomNo);
        UiFactory.textColumn(judgeCol, h -> h.judgeName().replaceFirst("^Justice\\s+", ""));
        UiFactory.chipColumn(statusCol, h -> h.status().getDisplayName(), h -> CaseDetailsDialog.hearingStyle(h.status()));
        UiFactory.actionColumn(actionsCol, this::rowButtons);
        UiFactory.onDoubleClick(table, h -> CaseDetailsDialog.show(h.caseId()));

        filter = new TableFilter<>(table, searchField, h -> h.caseNumber() + " " + h.caseTitle() + " " + h.judgeName());
        filter.bindCount(countLabel, "hearings");

        fromDate.setValue(LocalDate.now());
        toDate.setValue(LocalDate.now().plusDays(14));
        roomFilter.getItems().add(ALL_ROOMS);
        courtroomService.findAll().forEach(r -> roomFilter.getItems().add("Room " + r.getRoomNo()));
        roomFilter.setValue(ALL_ROOMS);

        // Any change to the filters reloads or re-filters the list
        fromDate.valueProperty().addListener((o, a, b) -> load());
        toDate.valueProperty().addListener((o, a, b) -> load());
        showCancelled.selectedProperty().addListener((o, a, b) -> load());
        roomFilter.valueProperty().addListener((o, a, room) ->
                filter.setExtra(h -> ALL_ROOMS.equals(room) || ("Room " + h.roomNo()).equals(room)));
        load();
    }

    private List<Button> rowButtons(Hearing h) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(UiFactory.rowButton("fth-folder", "Open case file", () -> CaseDetailsDialog.show(h.caseId())));
        if (h.status() == HearingStatus.SCHEDULED && !h.date().isBefore(LocalDate.now())) {
            buttons.add(UiFactory.rowButton("fth-clock", "Move to another date", () -> reschedule(h)));
            buttons.add(UiFactory.rowButton("fth-x-circle", "Cancel hearing", () -> cancel(h)));
        }
        return buttons;
    }

    private void load() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) return;
        if (to.isBefore(from)) {
            toDate.setValue(from);
            return;
        }
        boolean cancelled = showCancelled.isSelected();
        BackgroundTask.run(() -> hearingService.findBetween(from, to, cancelled), rows -> filter.setItems(rows));
    }

    // ------------------------------------------------------------------ actions

    @FXML
    private void handleSchedule() {
        List<CourtCase> cases = caseService.findActive().stream()
                .filter(c -> c.getStatus() != CaseStatus.RESERVED)
                .toList();
        FormDialog form = new FormDialog("List a hearing", "Add a further hearing for a registered case.", "List hearing");
        ComboBox<CourtCase> caseBox = form.add("Case", new ComboBox<>());
        caseBox.getItems().setAll(cases);
        caseBox.setMaxWidth(Double.MAX_VALUE);
        caseBox.setPromptText("Choose a case");
        caseBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CourtCase c) {
                return c == null ? "" : c.getCaseNumber() + "   " + c.getTitle();
            }

            @Override
            public CourtCase fromString(String s) {
                return null;
            }
        });
        Label judge = form.add("Judge", new Label("-"));

        SlotPicker slots = new SlotPicker(hearingService, LocalDate.now());
        slots.addTo(form, "Date");
        slots.setParticipants(() -> {
            CourtCase c = caseBox.getValue();
            return c == null ? null : new SlotPicker.Participants(c.getJudgeId(), c.getCourtroomId(), c.getFiledById());
        });
        caseBox.valueProperty().addListener((o, a, c) -> {
            judge.setText(c == null ? "-" : c.getJudgeName() + "  ·  Room " + c.getRoomNo());
            slots.refresh();
        });

        if (form.showAndSave(() -> {
            if (caseBox.getValue() == null) throw new ValidationException("Choose a case.");
            hearingService.schedule(caseBox.getValue(), slots.date(), slots.time());
        })) {
            Notifier.success("Hearing listed");
            load();
        }
    }

    private void reschedule(Hearing h) {
        FormDialog form = new FormDialog("Move hearing", h.caseNumber() + "  ·  " + h.caseTitle(), "Move hearing");
        form.add("Now listed", new Label(h.date().format(DATE) + " at " + h.time().format(TIME).toLowerCase()
                + "  ·  Room " + h.roomNo()));
        SlotPicker slots = new SlotPicker(hearingService, LocalDate.now());
        slots.setIgnoreHearingId(h.id());
        slots.addTo(form, "New date");
        slots.setParticipants(() -> new SlotPicker.Participants(h.judgeId(), h.courtroomId(), h.lawyerId()));
        if (form.showAndSave(() -> hearingService.reschedule(h, slots.date(), slots.time()))) {
            Notifier.success("Hearing moved to " + slots.date().format(DATE));
            load();
        }
    }

    private void cancel(Hearing h) {
        if (!AlertUtil.confirm("Cancel hearing", "Cancel the hearing of " + h.caseNumber() + " on "
                + h.date().format(DATE) + "?\nThe case will need a new date.")) {
            return;
        }
        try {
            hearingService.cancel(h);
            Notifier.success("Hearing cancelled");
            load();
        } catch (ValidationException e) {
            AlertUtil.warning("Can't cancel", e.getMessage());
        }
    }

    /** Lets the cause list screen reuse the room names. */
    static List<String> roomNames(List<Courtroom> rooms) {
        return rooms.stream().map(r -> "Room " + r.getRoomNo()).toList();
    }
}
