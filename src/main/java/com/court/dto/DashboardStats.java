package com.court.dto;

import java.time.LocalDate;

/**
 * Numbers shown in the stat cards of each dashboard.
 * Nested records keep the three related shapes together in one file.
 */
public final class DashboardStats {

    private DashboardStats() {
    }

    public record Registrar(
            int pendingCases,
            int filedThisWeek,
            int hearingsToday,
            int courtroomsInUse,
            int awaitingScrutiny,
            int scrutinyOverdue,
            int disposedLast30Days,
            int disposalRatePercent) {
    }

    public record Judge(
            int hearingsToday,
            int pendingCases,
            int reservedCases,
            int disposedLast30Days) {
    }

    public record Lawyer(
            int activeCases,
            int hearingsThisWeek,
            LocalDate nextHearing,     // null when nothing is scheduled
            int returnedCases,
            int disposedCases) {
    }
}
