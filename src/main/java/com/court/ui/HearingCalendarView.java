package com.court.ui;

import com.court.model.Hearing;
import com.court.model.HearingStatus;
import com.court.util.UiFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A diary of hearings: one card per court day, each listing that day's hearings.
 * Built in code (no FXML) because the number of cards depends on the data.
 */
public final class HearingCalendarView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private HearingCalendarView() {
    }

    /**
     * @param showJudge true for lawyers (who appear before different judges)
     */
    public static Node build(String title, String subtitle, List<Hearing> hearings, LocalDate from, int days,
                             Map<LocalDate, String> holidays, boolean showJudge) {
        // TreeMap groups hearings by date and keeps the dates in order
        Map<LocalDate, List<Hearing>> byDay = hearings.stream()
                .filter(h -> h.status() != HearingStatus.CANCELLED)
                .collect(Collectors.groupingBy(Hearing::date, TreeMap::new, Collectors.toList()));

        FlowPane cards = new FlowPane(14, 14);
        for (int i = 0; i < days; i++) {
            LocalDate day = from.plusDays(i);
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            cards.getChildren().add(dayCard(day, byDay.getOrDefault(day, List.of()), holidays.get(day), showJudge));
        }

        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        VBox page = new VBox(18, new VBox(3, heading, sub), cards);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 32, 32, 32));

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private static VBox dayCard(LocalDate day, List<Hearing> list, String holiday, boolean showJudge) {
        Label title = new Label(day.equals(LocalDate.now()) ? "Today, " + day.format(DateTimeFormatter.ofPattern("d MMM"))
                : day.format(DAY));
        title.getStyleClass().add("day-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = UiFactory.chip(list.size() + (list.size() == 1 ? " hearing" : " hearings"),
                list.isEmpty() ? "chip-neutral" : "chip-gold");
        HBox header = new HBox(8, title, spacer, count);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(header);
        card.getStyleClass().add("day-card");
        if (day.equals(LocalDate.now())) card.getStyleClass().add("today");
        card.setPrefWidth(300);

        if (holiday != null) {
            Label h = new Label("Court holiday: " + holiday);
            h.getStyleClass().add("row-subtitle");
            card.getChildren().add(h);
        } else if (list.isEmpty()) {
            Label free = new Label("No hearings listed.");
            free.getStyleClass().add("row-subtitle");
            card.getChildren().add(free);
        }
        for (Hearing h : list) {
            Label time = new Label(h.time().format(TIME).toLowerCase());
            time.getStyleClass().add("case-number");
            time.setMinWidth(70);
            String sub = "Room " + h.roomNo() + (showJudge ? "  ·  " + h.judgeName() : "")
                    + (h.status() == HearingStatus.COMPLETED ? "  ·  order passed" : "");
            HBox row = UiFactory.listRow(time, h.caseNumber() + "  " + h.caseTitle(), sub, null);
            row.setCursor(Cursor.HAND);
            row.setOnMouseClicked(e -> CaseDetailsDialog.show(h.caseId()));
            card.getChildren().add(row);
        }
        return card;
    }
}
