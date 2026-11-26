package com.court.ui;

import com.court.service.HearingService;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A date picker plus a time-slot list that only offers slots where the judge, the courtroom
 * and the lawyer are all free. Sundays, holidays and past days are greyed out in the calendar.
 * Used when registering a case, listing a hearing, moving one, and fixing the next date in an order.
 */
public final class SlotPicker {

    /** Who must be free: judge id, courtroom id and lawyer id. */
    public record Participants(int judgeId, int courtroomId, int lawyerId) {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private final HearingService hearingService;
    private final Map<LocalDate, String> holidays;
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<LocalTime> slotBox = new ComboBox<>();
    private final Label statusLabel = new Label();
    private Supplier<Participants> participants = () -> null;
    private Integer ignoreHearingId;

    public SlotPicker(HearingService hearingService, LocalDate earliest) {
        this.hearingService = hearingService;
        this.holidays = hearingService.holidays();

        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setEditable(false);
        // DateCell factory: disable days the court is closed
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate day, boolean empty) {
                super.updateItem(day, empty);
                getStyleClass().remove("holiday-cell");   // cells are reused, so reset first
                setTooltip(null);
                if (empty || day == null) return;
                boolean closed = day.isBefore(earliest) || day.getDayOfWeek() == DayOfWeek.SUNDAY
                        || holidays.containsKey(day);
                setDisable(closed);
                if (holidays.containsKey(day)) {
                    setTooltip(new Tooltip(holidays.get(day)));
                    getStyleClass().add("holiday-cell");
                }
            }
        });
        datePicker.setValue(nextOpenDay(earliest, holidays));

        slotBox.setMaxWidth(Double.MAX_VALUE);
        slotBox.setPromptText("Choose a time");
        slotBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(TIME).toLowerCase();
            }

            @Override
            public LocalTime fromString(String s) {
                return null;   // not editable
            }
        });
        slotBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LocalTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Choose a time" : t.format(TIME).toLowerCase());
            }
        });
        statusLabel.getStyleClass().add("hint-label");

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refresh());
    }

    /** Tells the picker whose diary to check; call {@link #refresh()} when the choice changes. */
    public void setParticipants(Supplier<Participants> participants) {
        this.participants = participants;
        refresh();
    }

    /** When moving an existing hearing, its own slot counts as free. */
    public void setIgnoreHearingId(Integer hearingId) {
        this.ignoreHearingId = hearingId;
    }

    public void refresh() {
        Participants p = participants.get();
        LocalDate day = datePicker.getValue();
        if (p == null || day == null) {
            slotBox.getItems().clear();
            statusLabel.setText("Choose the judge and date first.");
            return;
        }
        List<LocalTime> free = hearingService.freeSlots(p.judgeId(), p.courtroomId(), p.lawyerId(), day, ignoreHearingId);
        slotBox.getItems().setAll(free);
        if (free.isEmpty()) {
            statusLabel.setText("No free slots that day. Try another date.");
        } else {
            slotBox.getSelectionModel().selectFirst();
            statusLabel.setText(free.size() + " free slot" + (free.size() == 1 ? "" : "s")
                    + " (judge, courtroom and lawyer all available)");
        }
    }

    /** Date, time and status stacked in one node, for forms that show or hide them together. */
    public VBox asNode() {
        return new VBox(6, datePicker, slotBox, statusLabel);
    }

    public void addTo(FormDialog form, String dateLabel) {
        form.add(dateLabel, datePicker);
        form.add("Time", new VBox(4, slotBox, statusLabel));
    }

    public LocalDate date() {
        return datePicker.getValue();
    }

    public LocalTime time() {
        return slotBox.getValue();
    }

    public DatePicker datePicker() {
        return datePicker;
    }

    /** The first day on or after {@code from} that is not a Sunday or a holiday. */
    public static LocalDate nextOpenDay(LocalDate from, Map<LocalDate, String> holidays) {
        LocalDate d = from;
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.containsKey(d)) {
            d = d.plusDays(1);
        }
        return d;
    }
}
