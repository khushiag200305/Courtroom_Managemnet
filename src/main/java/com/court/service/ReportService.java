package com.court.service;

import com.court.dao.ReportDao;
import com.court.dao.impl.ReportDaoImpl;
import com.court.dto.AuditEntry;
import com.court.model.CaseStatus;
import com.court.util.AppConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Collects the figures for the Reports screen and exports them as CSV.
 */
public class ReportService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy");

    private final ReportDao reportDao = new ReportDaoImpl();

    public record ReportData(
            Map<YearMonth, Integer> filings,
            Map<YearMonth, Integer> disposals,
            Map<CaseStatus, Integer> byStatus,
            Map<String, Integer> pendingByJudge,
            double averageDaysToDisposal,
            List<AuditEntry> activity) {

        public int totalCases() {
            return byStatus.values().stream().mapToInt(Integer::intValue).sum();
        }

        public int count(CaseStatus status) {
            return byStatus.getOrDefault(status, 0);
        }

        /** Disposed cases as a percentage of all registered cases (Math.round for a whole number). */
        public int disposalRatePercent() {
            int registered = totalCases() - count(CaseStatus.FILED) - count(CaseStatus.DEFECTIVE);
            return registered == 0 ? 0 : (int) Math.round(count(CaseStatus.DISPOSED) * 100.0 / registered);
        }
    }

    public ReportData load(int months) {
        return new ReportData(
                reportDao.filingsByMonth(months),
                reportDao.disposalsByMonth(months),
                reportDao.countByStatus(),
                reportDao.pendingByJudge(),
                reportDao.averageDaysToDisposal(),
                reportDao.recentActivity(15));
    }

    /** Writes reports/court-report-DATE.csv with three sections. */
    public Path exportCsv(ReportData data) throws IOException {
        Path folder = Path.of(AppConfig.get("reports.folder", "reports"));
        Files.createDirectories(folder);
        Path file = folder.resolve("court-report-" + LocalDate.now() + ".csv");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("Court report," + AppConfig.get("court.name", "District Court").replace(",", " ")
                    + "," + LocalDate.now());
            out.newLine();
            out.newLine();
            out.write("Cases by status,count");
            out.newLine();
            for (Map.Entry<CaseStatus, Integer> e : data.byStatus().entrySet()) {
                out.write(e.getKey().getDisplayName() + "," + e.getValue());
                out.newLine();
            }
            out.newLine();
            out.write("Month,filed,disposed");
            out.newLine();
            for (YearMonth m : data.filings().keySet()) {
                out.write(m.format(MONTH) + "," + data.filings().get(m) + "," + data.disposals().getOrDefault(m, 0));
                out.newLine();
            }
            out.newLine();
            out.write("Judge,active cases");
            out.newLine();
            for (Map.Entry<String, Integer> e : data.pendingByJudge().entrySet()) {
                out.write(e.getKey() + "," + e.getValue());
                out.newLine();
            }
            out.newLine();
            out.write("Average days from filing to disposal," + Math.round(data.averageDaysToDisposal()));
            out.newLine();
            out.write("Disposal rate (%)," + data.disposalRatePercent());
            out.newLine();
        }
        return file;
    }
}
