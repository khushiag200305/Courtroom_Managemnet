package com.court.dao;

import com.court.dto.AuditEntry;
import com.court.model.CaseStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Figures for the Reports screen.
 */
public interface ReportDao {

    /** Cases filed per month for the last {@code months} months (oldest first, empty months as 0). */
    Map<YearMonth, Integer> filingsByMonth(int months);

    Map<YearMonth, Integer> disposalsByMonth(int months);

    Map<CaseStatus, Integer> countByStatus();

    /** Judge name -> number of registered and pending cases, largest first. */
    Map<String, Integer> pendingByJudge();

    /** Average number of days from filing to disposal; 0 when nothing is disposed. */
    double averageDaysToDisposal();

    /** Court holidays, sorted by date. */
    Map<LocalDate, String> holidays();

    List<AuditEntry> recentActivity(int limit);
}
