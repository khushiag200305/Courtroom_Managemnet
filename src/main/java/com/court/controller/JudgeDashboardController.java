package com.court.controller;

import com.court.dto.CaseSummary;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.model.CaseStatus;
import com.court.model.Judge;
import com.court.model.User;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Judge's dashboard: today's cause list, the coming week and reserved judgments.
 */
public class JudgeDashboardController extends DashboardController {

    @FXML private Label roomSubtitle;
    @FXML private Label todayValue;
    @FXML private Label todayTrend;
    @FXML private Label pendingValue;
    @FXML private Label reservedValue;
    @FXML private Label reservedTrend;
    @FXML private Label disposedValue;
    @FXML private Label listChip;
    @FXML private TableView<HearingRow> hearingsTable;
    @FXML private TableColumn<HearingRow, String> timeCol;
    @FXML private TableColumn<HearingRow, String> caseNoCol;
    @FXML private TableColumn<HearingRow, String> titleCol;
    @FXML private TableColumn<HearingRow, CaseStatus> statusCol;
    @FXML private VBox weekBars;
    @FXML private VBox reservedList;

    private record HomeData(
            DashboardStats.Judge stats,
            List<HearingRow> today,
            Map<LocalDate, Integer> week,
            List<CaseSummary> reserved) {
    }

    private boolean tableReady = false;

    @Override
    protected void onDashboardReady(User user) {
        Judge judge = (Judge) user;   // safe: only Judge objects open this dashboard
        roomSubtitle.setText(judge.hasCourtroom()
                ? judge.getDesignation() + " · Room " + judge.getCourtroomName()
                : judge.getDesignation() + " · no courtroom assigned yet");

        if (!tableReady) {
            UiFactory.textColumn(timeCol, row -> row.time().format(TIME));
            UiFactory.textColumn(caseNoCol, HearingRow::caseNumber);
            UiFactory.textColumn(titleCol, row -> (row.urgent() ? "● " : "") + row.title());
            UiFactory.statusColumn(statusCol, HearingRow::status);
            caseNoCol.getStyleClass().add("mono-cell");
            UiFactory.fitToRows(hearingsTable, 20, "No hearings are listed before you today.");
            tableReady = true;
        }

        int id = judge.getId();
        BackgroundTask.run(
                () -> new HomeData(
                        dashboardDao.judgeStats(id),
                        dashboardDao.todaysHearingsForJudge(id),
                        dashboardDao.judgeUpcomingLoad(id, 7),
                        dashboardDao.reservedCases(id)),
                this::show,
                this::showLoadError);
    }

    private void show(HomeData data) {
        DashboardStats.Judge s = data.stats();
        todayValue.setText(String.valueOf(s.hearingsToday()));
        todayTrend.setText(data.today().isEmpty() ? "no hearings today"
                : "first at " + data.today().get(0).time().format(TIME));
        pendingValue.setText(String.valueOf(s.pendingCases()));
        reservedValue.setText(String.valueOf(s.reservedCases()));
        reservedTrend.setText(s.reservedCases() == 0 ? "nothing pending" : "awaiting your judgment");
        disposedValue.setText(String.valueOf(s.disposedLast30Days()));

        hearingsTable.getItems().setAll(data.today());
        listChip.setText(data.today().size() + " cases");

        showWeek(data.week());
        showReserved(data.reserved());
    }

    private void showWeek(Map<LocalDate, Integer> week) {
        weekBars.getChildren().clear();
        int max = week.isEmpty() ? 0 : Math.max(1, Collections.max(week.values()));
        // Map.entrySet() of a TreeMap comes back sorted by date
        for (Map.Entry<LocalDate, Integer> day : week.entrySet()) {
            weekBars.getChildren().add(UiFactory.barRow(day.getKey().format(SHORT_DATE),
                    day.getValue(), max, "bar-gold"));
        }
    }

    private void showReserved(List<CaseSummary> reserved) {
        reservedList.getChildren().clear();
        if (reserved.isEmpty()) {
            reservedList.getChildren().add(UiFactory.emptyState("fth-check-circle", "No judgments are reserved."));
            return;
        }
        for (CaseSummary c : reserved) {
            long days = c.date() == null ? 0 : ChronoUnit.DAYS.between(c.date(), LocalDate.now());
            String subtitle = c.caseNumber() + " · " + c.type().getDisplayName()
                    + " · reserved " + UiFactory.relativeDays(c.date());
            reservedList.getChildren().add(UiFactory.listRow(
                    UiFactory.icon("fth-file-text", 18), c.title(), subtitle,
                    UiFactory.chip(days + " days", days > 30 ? "chip-red" : "chip-gold")));
        }
    }
}
