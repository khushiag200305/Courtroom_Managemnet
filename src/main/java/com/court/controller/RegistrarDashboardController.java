package com.court.controller;

import com.court.dto.CaseSummary;
import com.court.dto.CourtroomLoad;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.model.Role;
import com.court.model.User;
import com.court.util.BackgroundTask;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
 * Registrar's dashboard: court-wide figures, today's hearings, courtroom use,
 * active cases by type and the scrutiny queue.
 */
public class RegistrarDashboardController extends DashboardController {

    private static final String[] BAR_STYLES = {"bar-navy", "bar-gold", "bar-purple", "bar-green"};

    @FXML private Label scrutinyBadge;
    @FXML private Button scrutinyButton;
    @FXML private Button hearingsButton;
    @FXML private Button causeListButton;
    @FXML private Button judgesButton;
    @FXML private Button lawyersButton;
    @FXML private Button courtroomsButton;
    @FXML private Button reportsButton;
    @FXML private Label pendingValue;
    @FXML private Label pendingTrend;
    @FXML private Label hearingsValue;
    @FXML private Label hearingsTrend;
    @FXML private Label scrutinyValue;
    @FXML private Label scrutinyTrend;
    @FXML private Label disposedValue;
    @FXML private Label disposedTrend;
    @FXML private Label hearingsChip;
    @FXML private TableView<HearingRow> hearingsTable;
    @FXML private TableColumn<HearingRow, String> timeCol;
    @FXML private TableColumn<HearingRow, String> caseNoCol;
    @FXML private TableColumn<HearingRow, String> titleCol;
    @FXML private TableColumn<HearingRow, String> roomCol;
    @FXML private TableColumn<HearingRow, String> judgeCol;
    @FXML private TableColumn<HearingRow, CaseStatus> statusCol;
    @FXML private VBox courtroomList;
    @FXML private VBox caseTypeBars;
    @FXML private VBox scrutinyList;

    /** Everything the home screen needs, loaded together on a background thread. */
    private record HomeData(
            DashboardStats.Registrar stats,
            List<HearingRow> hearings,
            List<CourtroomLoad> rooms,
            Map<CaseType, Integer> byType,
            List<CaseSummary> scrutiny) {
    }

    private boolean tableReady = false;

    @Override
    protected void onDashboardReady(User user) {
        if (!tableReady) {
            setUpTable();
            tableReady = true;
        }
        BackgroundTask.run(
                () -> new HomeData(
                        dashboardDao.registrarStats(),
                        dashboardDao.todaysHearings(),
                        dashboardDao.courtroomLoad(),
                        dashboardDao.caseCountByType(),
                        dashboardDao.scrutinyQueue(5)),
                this::show,
                this::showLoadError);
    }

    // ------------------------------------------------------------------ sidebar

    @FXML
    private void openScrutiny() {
        loadContent("scrutiny.fxml", scrutinyButton);
    }

    @FXML
    private void openHearings() {
        loadContent("hearings.fxml", hearingsButton);
    }

    @FXML
    private void openCauseList() {
        loadContent("cause_list.fxml", causeListButton);
    }

    @FXML
    private void openJudges() {
        UsersController controller = loadContent("users.fxml", judgesButton);
        if (controller != null) controller.init(Role.JUDGE);
    }

    @FXML
    private void openLawyers() {
        UsersController controller = loadContent("users.fxml", lawyersButton);
        if (controller != null) controller.init(Role.LAWYER);
    }

    @FXML
    private void openCourtrooms() {
        loadContent("courtrooms.fxml", courtroomsButton);
    }

    @FXML
    private void openReports() {
        loadContent("reports.fxml", reportsButton);
    }

    // ------------------------------------------------------------------ home screen

    private void setUpTable() {
        UiFactory.textColumn(timeCol, row -> row.time().format(TIME));
        UiFactory.textColumn(caseNoCol, HearingRow::caseNumber);
        UiFactory.textColumn(titleCol, row -> (row.urgent() ? "● " : "") + row.title());
        UiFactory.textColumn(roomCol, HearingRow::roomNo);
        UiFactory.textColumn(judgeCol, row -> withoutTitle(row.judgeName()));
        UiFactory.statusColumn(statusCol, HearingRow::status);
        caseNoCol.getStyleClass().add("mono-cell");
        UiFactory.fitToRows(hearingsTable, 20, "No hearings are listed for today.");
    }

    private void show(HomeData data) {
        showStats(data.stats());

        hearingsTable.getItems().setAll(data.hearings());
        hearingsChip.setText(data.hearings().size() + " listed");

        showCourtrooms(data.rooms());
        showCaseTypes(data.byType());
        showScrutiny(data.scrutiny());
    }

    private void showStats(DashboardStats.Registrar s) {
        pendingValue.setText(String.valueOf(s.pendingCases()));
        setTrend(pendingTrend, "+" + s.filedThisWeek() + " filed this week", s.filedThisWeek() > 0 ? "trend-good" : null);

        hearingsValue.setText(String.valueOf(s.hearingsToday()));
        setTrend(hearingsTrend, "across " + s.courtroomsInUse() + (s.courtroomsInUse() == 1 ? " courtroom" : " courtrooms"), null);

        scrutinyValue.setText(String.valueOf(s.awaitingScrutiny()));
        if (s.scrutinyOverdue() > 0) {
            setTrend(scrutinyTrend, s.scrutinyOverdue() + " waiting more than 3 days", "trend-bad");
        } else {
            setTrend(scrutinyTrend, "all within 3 days", "trend-good");
        }

        disposedValue.setText(String.valueOf(s.disposedLast30Days()));
        setTrend(disposedTrend, s.disposalRatePercent() + "% of all cases decided", null);

        scrutinyBadge.setText(String.valueOf(s.awaitingScrutiny()));
        scrutinyBadge.setVisible(s.awaitingScrutiny() > 0);
    }

    private void setTrend(Label label, String text, String style) {
        label.setText(text);
        label.getStyleClass().removeAll("trend-good", "trend-bad");
        if (style != null) {
            label.getStyleClass().add(style);
        }
    }

    private void showCourtrooms(List<CourtroomLoad> rooms) {
        courtroomList.getChildren().clear();
        for (CourtroomLoad room : rooms) {
            boolean sitting = room.hasJudge() && room.hearingsToday() > 0;
            String dotStyle = !room.hasJudge() ? "dot-red" : (sitting ? "dot-green" : "dot-amber");
            String subtitle = room.hasJudge() ? room.judgeName() : "No judge assigned";
            Label count = UiFactory.chip(room.hearingsToday() + " today",
                    room.hearingsToday() > 0 ? "chip-neutral" : "chip-gold");
            courtroomList.getChildren().add(UiFactory.listRow(
                    UiFactory.dot(dotStyle), "Room " + room.roomNo() + " · " + room.roomName(), subtitle, count));
        }
        if (rooms.isEmpty()) {
            courtroomList.getChildren().add(UiFactory.emptyState("fth-layers", "No courtrooms added yet."));
        }
    }

    private void showCaseTypes(Map<CaseType, Integer> byType) {
        caseTypeBars.getChildren().clear();
        int max = byType.isEmpty() ? 0 : Collections.max(byType.values());
        int i = 0;
        for (CaseType type : CaseType.values()) {
            int count = byType.getOrDefault(type, 0);
            if (count == 0 && type == CaseType.OTHER) continue;   // hide "Other" when unused
            caseTypeBars.getChildren().add(UiFactory.barRow(type.getDisplayName(), count, max,
                    BAR_STYLES[i++ % BAR_STYLES.length]));
        }
    }

    private void showScrutiny(List<CaseSummary> queue) {
        scrutinyList.getChildren().clear();
        if (queue.isEmpty()) {
            scrutinyList.getChildren().add(UiFactory.emptyState("fth-check-circle", "No new filings waiting."));
            return;
        }
        for (CaseSummary c : queue) {
            long waiting = ChronoUnit.DAYS.between(c.date(), LocalDate.now());
            String subtitle = c.type().getDisplayName() + " · " + c.note() + " · filed " + UiFactory.relativeDays(c.date());
            Label chip;
            if (c.urgent()) {
                chip = UiFactory.chip("Urgent", "chip-red");
            } else if (waiting > 3) {
                chip = UiFactory.chip(waiting + " days", "chip-gold");
            } else {
                chip = UiFactory.chip("New", "chip-green");
            }
            scrutinyList.getChildren().add(UiFactory.listRow(
                    UiFactory.avatar(c.note(), null), c.title(), subtitle, chip));
        }
    }
}
