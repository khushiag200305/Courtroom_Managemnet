package com.court.controller;

import com.court.dto.CaseSummary;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.model.CaseStatus;
import com.court.model.Lawyer;
import com.court.model.User;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

/**
 * Lawyer's dashboard: own cases, upcoming hearings and returned filings.
 */
public class LawyerDashboardController extends DashboardController {

    @FXML private Label returnedBadge;
    @FXML private Label activeValue;
    @FXML private Label weekValue;
    @FXML private Label weekTrend;
    @FXML private Label returnedValue;
    @FXML private Label returnedTrend;
    @FXML private Label disposedValue;
    @FXML private TableView<HearingRow> hearingsTable;
    @FXML private TableColumn<HearingRow, String> dateCol;
    @FXML private TableColumn<HearingRow, String> timeCol;
    @FXML private TableColumn<HearingRow, String> caseNoCol;
    @FXML private TableColumn<HearingRow, String> titleCol;
    @FXML private TableColumn<HearingRow, String> roomCol;
    @FXML private TableColumn<HearingRow, CaseStatus> statusCol;
    @FXML private Label profileAvatar;
    @FXML private Label profileName;
    @FXML private Label barIdLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private VBox returnedList;

    private record HomeData(DashboardStats.Lawyer stats, List<HearingRow> upcoming, List<CaseSummary> returned) {
    }

    private boolean tableReady = false;

    @Override
    protected void onDashboardReady(User user) {
        Lawyer lawyer = (Lawyer) user;
        profileAvatar.setText(UiFactory.initials(lawyer.getName()));
        profileName.setText(lawyer.getName());
        barIdLabel.setText(lawyer.getBarId());
        emailLabel.setText(Objects.requireNonNullElse(lawyer.getEmail(), "No email on record"));
        phoneLabel.setText(Objects.requireNonNullElse(lawyer.getPhone(), "No phone on record"));

        if (!tableReady) {
            UiFactory.textColumn(dateCol, row -> row.date().format(SHORT_DATE));
            UiFactory.textColumn(timeCol, row -> row.time().format(TIME));
            UiFactory.textColumn(caseNoCol, HearingRow::caseNumber);
            UiFactory.textColumn(titleCol, row -> (row.urgent() ? "● " : "") + row.title());
            UiFactory.textColumn(roomCol, HearingRow::roomNo);
            UiFactory.statusColumn(statusCol, HearingRow::status);
            caseNoCol.getStyleClass().add("mono-cell");
            UiFactory.fitToRows(hearingsTable, 15, "No hearings in the next 30 days.");
            tableReady = true;
        }

        int id = lawyer.getId();
        BackgroundTask.run(
                () -> new HomeData(
                        dashboardDao.lawyerStats(id),
                        dashboardDao.lawyerUpcomingHearings(id, 30),
                        dashboardDao.returnedCases(id)),
                this::show,
                this::showLoadError);
    }

    private void show(HomeData data) {
        DashboardStats.Lawyer s = data.stats();
        activeValue.setText(String.valueOf(s.activeCases()));
        weekValue.setText(String.valueOf(s.hearingsThisWeek()));
        weekTrend.setText(s.nextHearing() == null ? "nothing scheduled"
                : "next " + UiFactory.relativeDays(s.nextHearing()) + " (" + s.nextHearing().format(SHORT_DATE) + ")");
        returnedValue.setText(String.valueOf(s.returnedCases()));
        returnedTrend.setText(s.returnedCases() == 0 ? "nothing to fix" : "need correction");
        returnedTrend.getStyleClass().removeAll("trend-good", "trend-bad");
        returnedTrend.getStyleClass().add(s.returnedCases() == 0 ? "trend-good" : "trend-bad");
        disposedValue.setText(String.valueOf(s.disposedCases()));

        returnedBadge.setText(String.valueOf(s.returnedCases()));
        returnedBadge.setVisible(s.returnedCases() > 0);

        hearingsTable.getItems().setAll(data.upcoming());
        showReturned(data.returned());
    }

    private void showReturned(List<CaseSummary> returned) {
        returnedList.getChildren().clear();
        if (returned.isEmpty()) {
            returnedList.getChildren().add(UiFactory.emptyState("fth-check-circle", "All your filings are in order."));
            return;
        }
        for (CaseSummary c : returned) {
            String subtitle = "Defect: " + c.note();
            returnedList.getChildren().add(UiFactory.listRow(
                    UiFactory.icon("fth-alert-triangle", 18), c.title() + "  ·  " + c.type().getDisplayName(),
                    subtitle, UiFactory.chip("Filed " + UiFactory.relativeDays(c.date()), "chip-red")));
        }
    }
}
