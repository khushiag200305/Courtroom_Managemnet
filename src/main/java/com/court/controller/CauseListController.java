package com.court.controller;

import com.court.model.CaseStatus;
import com.court.model.Courtroom;
import com.court.model.Hearing;
import com.court.service.CauseListService;
import com.court.service.CourtroomService;
import com.court.ui.CaseDetailsDialog;
import com.court.ui.Notifier;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Registrar's cause list for any day (cause_list.fxml), with text and CSV export.
 */
public class CauseListController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String ALL_ROOMS = "All courtrooms";

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> roomFilter;
    @FXML private Label countLabel;
    @FXML private FlowPane roomSummary;
    @FXML private TableView<Hearing> table;
    @FXML private TableColumn<Hearing, String> srCol;
    @FXML private TableColumn<Hearing, String> roomCol;
    @FXML private TableColumn<Hearing, String> timeCol;
    @FXML private TableColumn<Hearing, String> caseNoCol;
    @FXML private TableColumn<Hearing, String> titleCol;
    @FXML private TableColumn<Hearing, String> judgeCol;
    @FXML private TableColumn<Hearing, CaseStatus> statusCol;

    private final CauseListService causeListService = new CauseListService();
    private final CourtroomService courtroomService = new CourtroomService();
    /** "Room 1" -> courtroom id; LinkedHashMap keeps the rooms in order for the ComboBox. */
    private final Map<String, Integer> roomIds = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        UiFactory.pageTable(table, "No cases are listed for this day.");
        UiFactory.textColumn(srCol, h -> String.valueOf(table.getItems().indexOf(h) + 1));
        UiFactory.textColumn(roomCol, h -> "Room " + h.roomNo());
        UiFactory.textColumn(timeCol, h -> h.time().format(TIME).toLowerCase());
        UiFactory.textColumn(caseNoCol, Hearing::caseNumber);
        caseNoCol.getStyleClass().add("mono-cell");
        UiFactory.textColumn(titleCol, Hearing::caseTitle);
        UiFactory.textColumn(judgeCol, Hearing::judgeName);
        UiFactory.statusColumn(statusCol, Hearing::caseStatus);
        UiFactory.onDoubleClick(table, h -> CaseDetailsDialog.show(h.caseId()));

        roomIds.put(ALL_ROOMS, null);
        for (Courtroom r : courtroomService.findAll()) {
            roomIds.put("Room " + r.getRoomNo() + " · " + r.getName(), r.getId());
        }
        roomFilter.getItems().setAll(roomIds.keySet());
        roomFilter.setValue(ALL_ROOMS);

        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((o, a, b) -> load());
        roomFilter.valueProperty().addListener((o, a, b) -> load());
        load();
    }

    private void load() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;
        Integer roomId = roomIds.get(roomFilter.getValue());
        BackgroundTask.run(() -> causeListService.causeList(date, roomId), list -> {
            table.getItems().setAll(list);
            countLabel.setText(list.size() + (list.size() == 1 ? " case" : " cases"));
            roomSummary.getChildren().clear();
            CauseListService.byCourtroom(list).forEach((court, hearings) ->
                    roomSummary.getChildren().add(UiFactory.chip(court + "  ·  " + hearings.size(), "chip-gold")));
        });
    }

    @FXML
    private void handleTomorrow() {
        datePicker.setValue(LocalDate.now().plusDays(1));
    }

    @FXML
    private void handleExportText() {
        export(() -> causeListService.exportText(datePicker.getValue(), List.copyOf(table.getItems())));
    }

    @FXML
    private void handleExportCsv() {
        export(() -> causeListService.exportCsv(datePicker.getValue(), List.copyOf(table.getItems())));
    }

    private void export(Callable<Path> work) {
        BackgroundTask.run(work, file -> {
            Notifier.success("Saved " + file.getFileName());
            if (AlertUtil.confirm("Cause list exported", "Saved to " + file.toAbsolutePath() + "\n\nOpen it now?")) {
                SceneManager.openFile(file);
            }
        });
    }
}
