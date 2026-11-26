package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.Courtroom;
import com.court.model.Hearing;
import com.court.model.Judge;
import com.court.model.Role;
import com.court.model.User;
import com.court.service.CourtroomService;
import com.court.service.HearingService;
import com.court.service.UserService;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.TableFilter;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registrar screen for courtrooms (courtrooms.fxml).
 */
public class CourtroomsController {

    /** One table row: the room, its judge's name and today's hearing count. */
    public record RoomRow(Courtroom room, String judgeName, long hearingsToday) {
    }

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private TableView<RoomRow> table;
    @FXML private TableColumn<RoomRow, String> roomCol;
    @FXML private TableColumn<RoomRow, String> nameCol;
    @FXML private TableColumn<RoomRow, String> judgeCol;
    @FXML private TableColumn<RoomRow, String> todayCol;
    @FXML private TableColumn<RoomRow, String> statusCol;
    @FXML private TableColumn<RoomRow, Void> actionsCol;

    private final CourtroomService courtroomService = new CourtroomService();
    private final UserService userService = new UserService();
    private final HearingService hearingService = new HearingService();
    private TableFilter<RoomRow> filter;

    @FXML
    private void initialize() {
        UiFactory.pageTable(table, "No courtrooms yet. Add the first one.");
        UiFactory.textColumn(roomCol, r -> r.room().getRoomNo());
        UiFactory.textColumn(nameCol, r -> r.room().getName());
        UiFactory.textColumn(judgeCol, r -> r.judgeName() == null ? "No judge assigned" : r.judgeName());
        UiFactory.textColumn(todayCol, r -> String.valueOf(r.hearingsToday()));
        UiFactory.chipColumn(statusCol, r -> r.room().isActive() ? "In use" : "Closed",
                r -> r.room().isActive() ? "chip-green" : "chip-neutral");
        UiFactory.actionColumn(actionsCol, r -> List.of(
                UiFactory.rowButton("fth-edit-2", "Edit", () -> openForm(r.room())),
                UiFactory.rowButton(r.room().isActive() ? "fth-lock" : "fth-unlock",
                        r.room().isActive() ? "Close courtroom" : "Reopen courtroom", () -> toggle(r.room())),
                UiFactory.rowButton("fth-trash-2", "Delete", () -> delete(r.room()))));
        UiFactory.onDoubleClick(table, r -> openForm(r.room()));

        filter = new TableFilter<>(table, searchField,
                r -> r.room().getRoomNo() + " " + r.room().getName() + " " + r.judgeName());
        filter.bindCount(countLabel, "courtrooms");
        load();
    }

    private void load() {
        BackgroundTask.run(() -> {
            // HashMap: room id -> judge name
            Map<Integer, String> judgeByRoom = new HashMap<>();
            for (User u : userService.findByRole(Role.JUDGE)) {
                if (u instanceof Judge j && j.hasCourtroom()) {
                    judgeByRoom.put(j.getCourtroomId(), j.getName());
                }
            }
            // Count today's hearings per room with Collectors.groupingBy
            Map<Integer, Long> today = hearingService.findBetween(LocalDate.now(), LocalDate.now(), false).stream()
                    .collect(Collectors.groupingBy(Hearing::courtroomId, Collectors.counting()));
            return courtroomService.findAll().stream()
                    .map(room -> new RoomRow(room, judgeByRoom.get(room.getId()), today.getOrDefault(room.getId(), 0L)))
                    .toList();
        }, rows -> filter.setItems(rows));
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    private void openForm(Courtroom existing) {
        boolean isNew = existing == null;
        FormDialog form = new FormDialog(isNew ? "Add courtroom" : "Edit courtroom",
                "Give the room number shown on the door and the name of the court.",
                isNew ? "Add courtroom" : "Save changes");
        TextField roomNo = form.add("Room number", new TextField(isNew ? "" : existing.getRoomNo()));
        roomNo.setPromptText("5");
        TextField name = form.add("Court name", new TextField(isNew ? "" : existing.getName()));
        name.setPromptText("Additional Civil Court");
        form.addNote("Assign a judge to the room from the Judges screen.");

        if (form.showAndSave(() -> {
            Courtroom room = isNew ? new Courtroom() : existing;
            room.setRoomNo(roomNo.getText());
            room.setName(name.getText());
            courtroomService.save(room);
        })) {
            Notifier.success(isNew ? "Courtroom added" : "Changes saved");
        }
        load();
    }

    private void toggle(Courtroom room) {
        boolean reopen = !room.isActive();
        if (!AlertUtil.confirm(reopen ? "Reopen courtroom" : "Close courtroom",
                (reopen ? "Reopen" : "Close") + " Room " + room.getRoomNo() + "?"
                        + (reopen ? "" : "\nNo new hearings can be listed in a closed room."))) {
            return;
        }
        try {
            courtroomService.setActive(room, reopen);
            Notifier.success("Room " + room.getRoomNo() + (reopen ? " reopened" : " closed"));
        } catch (ValidationException e) {
            AlertUtil.warning("Couldn't change the courtroom", e.getMessage());
        }
        load();
    }

    private void delete(Courtroom room) {
        if (!AlertUtil.confirm("Delete courtroom", "Delete Room " + room.getRoomNo() + " permanently?")) {
            return;
        }
        try {
            courtroomService.delete(room);
            Notifier.success("Courtroom deleted");
            load();
        } catch (ValidationException e) {
            AlertUtil.warning("Can't delete this courtroom", e.getMessage());
        }
    }
}
