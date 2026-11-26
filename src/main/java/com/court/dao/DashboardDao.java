package com.court.dao;

import com.court.dto.CaseSummary;
import com.court.dto.CourtroomLoad;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.model.CaseType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only queries that feed the three dashboards and the public case-status look-up.
 */
public interface DashboardDao {

    // ----- Registrar
    DashboardStats.Registrar registrarStats();

    List<HearingRow> todaysHearings();

    List<CourtroomLoad> courtroomLoad();

    Map<CaseType, Integer> caseCountByType();

    List<CaseSummary> scrutinyQueue(int limit);

    // ----- Judge
    DashboardStats.Judge judgeStats(int judgeId);

    List<HearingRow> todaysHearingsForJudge(int judgeId);

    List<CaseSummary> reservedCases(int judgeId);

    /** Number of hearings per day for the next {@code days} days (dates in order). */
    Map<LocalDate, Integer> judgeUpcomingLoad(int judgeId, int days);

    // ----- Lawyer
    DashboardStats.Lawyer lawyerStats(int lawyerId);

    List<HearingRow> lawyerUpcomingHearings(int lawyerId, int days);

    List<CaseSummary> returnedCases(int lawyerId);

    // ----- Public (no login)
    Optional<CaseSummary> findPublicStatus(String caseNumber);
}
