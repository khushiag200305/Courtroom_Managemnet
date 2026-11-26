package com.court.controller;

import com.court.dto.AuditEntry;
import com.court.model.CaseStatus;
import com.court.service.BackupService;
import com.court.service.ReportService;
import com.court.ui.Notifier;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Registrar's reports (reports.fxml): charts, judge workload, audit log, CSV export and the
 * serialized archive of decided cases.
 */
public class ReportsController {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yy");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, hh:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    @FXML private Label totalValue;
    @FXML private Label totalTrend;
    @FXML private Label rateValue;
    @FXML private Label daysValue;
    @FXML private Label defectiveValue;
    @FXML private BarChart<String, Number> monthChart;
    @FXML private PieChart statusChart;
    @FXML private VBox judgeBars;
    @FXML private ListView<AuditEntry> activityList;
    @FXML private Label backupInfo;

    private final ReportService reportService = new ReportService();
    private final BackupService backupService = new BackupService();
    private ReportService.ReportData data;

    @FXML
    private void initialize() {
        activityList.setPlaceholder(UiFactory.emptyState("fth-activity", "No activity recorded yet."));
        activityList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AuditEntry a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setGraphic(null);
                    return;
                }
                Label what = new Label(a.action().replace('_', ' ').toLowerCase() + (a.details() == null ? "" : ": " + a.details()));
                what.getStyleClass().add("row-title");
                Label who = new Label(a.userName() + "  ·  " + (a.time() == null ? "" : a.time().format(WHEN)));
                who.getStyleClass().add("row-subtitle");
                setGraphic(new VBox(2, what, who));
            }
        });
        load();
        showBackupInfo();
    }

    private void load() {
        BackgroundTask.run(() -> reportService.load(6), this::show);
    }

    private void show(ReportService.ReportData d) {
        this.data = d;
        totalValue.setText(String.valueOf(d.totalCases()));
        totalTrend.setText(d.count(CaseStatus.PENDING) + d.count(CaseStatus.REGISTERED) + " pending, "
                + d.count(CaseStatus.DISPOSED) + " decided");
        rateValue.setText(d.disposalRatePercent() + "%");
        daysValue.setText(String.valueOf(Math.round(d.averageDaysToDisposal())));
        defectiveValue.setText(String.valueOf(d.count(CaseStatus.DEFECTIVE)));

        // Bar chart: two series over the same months
        XYChart.Series<String, Number> filed = new XYChart.Series<>();
        filed.setName("Filed");
        XYChart.Series<String, Number> decided = new XYChart.Series<>();
        decided.setName("Decided");
        for (Map.Entry<YearMonth, Integer> m : d.filings().entrySet()) {
            String label = m.getKey().format(MONTH);
            filed.getData().add(new XYChart.Data<>(label, m.getValue()));
            decided.getData().add(new XYChart.Data<>(label, d.disposals().getOrDefault(m.getKey(), 0)));
        }
        monthChart.getData().setAll(List.of(filed, decided));

        // Pie chart: only stages that have cases
        statusChart.getData().clear();
        for (CaseStatus s : CaseStatus.values()) {
            int count = d.count(s);
            if (count > 0) {
                statusChart.getData().add(new PieChart.Data(s.getDisplayName() + " (" + count + ")", count));
            }
        }

        judgeBars.getChildren().clear();
        int max = d.pendingByJudge().isEmpty() ? 0 : Collections.max(d.pendingByJudge().values());
        d.pendingByJudge().forEach((judge, count) -> judgeBars.getChildren().add(
                UiFactory.barRow(judge.replaceFirst("^Justice\\s+", ""), count, max, "bar-gold")));

        activityList.getItems().setAll(d.activity());
    }

    private void showBackupInfo() {
        BackgroundTask.run(backupService::existingBackups, files -> backupInfo.setText(files.isEmpty()
                ? "Save every decided case, with its parties and orders, to a file in " + backupService.backupFolder()
                : files.size() + " backup file(s) in " + backupService.backupFolder()
                + ". Latest: " + files.get(0).getFileName()));
    }

    // ------------------------------------------------------------------ actions

    @FXML
    private void handleExport() {
        if (data == null) return;
        BackgroundTask.run(() -> reportService.exportCsv(data), file -> {
            Notifier.success("Saved " + file.getFileName());
            if (AlertUtil.confirm("Report exported", "Saved to " + file.toAbsolutePath() + "\n\nOpen it now?")) {
                SceneManager.openFile(file);
            }
        });
    }

    @FXML
    private void handleBackup() {
        BackgroundTask.run(backupService::backupDisposedCases, file -> {
            Notifier.success("Backup saved: " + file.getFileName());
            showBackupInfo();
        });
    }

    @FXML
    private void handleOpenBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open a backup of decided cases");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Case archive", "*.ser"));
        File folder = backupService.backupFolder().toFile();
        if (folder.isDirectory()) chooser.setInitialDirectory(folder);
        File file = chooser.showOpenDialog(SceneManager.getStage());
        if (file == null) return;

        BackgroundTask.run(() -> backupService.readBackup(file.toPath()), this::showArchive,
                error -> AlertUtil.error("Can't open this file",
                        error instanceof IOException ? error.getMessage() : "The file could not be read."));
    }

    /** Shows the deserialized archive in a table. */
    private void showArchive(BackupService.Archive archive) {
        TableView<BackupService.ArchivedCase> table = new TableView<>();
        TableColumn<BackupService.ArchivedCase, String> no = new TableColumn<>("Case no.");
        TableColumn<BackupService.ArchivedCase, String> title = new TableColumn<>("Parties");
        TableColumn<BackupService.ArchivedCase, String> judge = new TableColumn<>("Judge");
        TableColumn<BackupService.ArchivedCase, String> decided = new TableColumn<>("Decided on");
        TableColumn<BackupService.ArchivedCase, String> orders = new TableColumn<>("Orders");
        UiFactory.textColumn(no, BackupService.ArchivedCase::caseNumber);
        UiFactory.textColumn(title, BackupService.ArchivedCase::title);
        UiFactory.textColumn(judge, BackupService.ArchivedCase::judge);
        UiFactory.textColumn(decided, c -> c.disposed() == null ? "-" : c.disposed().format(DATE));
        UiFactory.textColumn(orders, c -> String.valueOf(c.orders().size()));
        table.getColumns().addAll(List.of(no, title, judge, decided, orders));
        UiFactory.pageTable(table, "The archive is empty.");
        table.getItems().setAll(archive.cases());
        table.setPrefSize(760, 380);

        Label info = new Label(archive.cases().size() + " decided cases, backed up by " + archive.createdBy()
                + " on " + archive.createdAt().format(WHEN));
        info.getStyleClass().add("page-subtitle");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Case archive");
        dialog.initOwner(SceneManager.getStage());
        dialog.getDialogPane().setContent(new VBox(12, info, table));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(SceneManager.css());
        dialog.setResizable(true);
        dialog.showAndWait();
    }
}
